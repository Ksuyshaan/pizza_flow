package com.pizzaflow.report.service;

import com.pizzaflow.delivery.entity.Courier;
import com.pizzaflow.delivery.repository.CourierRepository;
import com.pizzaflow.delivery.repository.PaymentRepository;
import com.pizzaflow.order.entity.Order;
import com.pizzaflow.order.entity.OrderItem;
import com.pizzaflow.order.entity.OrderStatus;
import com.pizzaflow.order.repository.OrderRepository;
import com.pizzaflow.report.dto.CategorySalesDto;
import com.pizzaflow.report.dto.RevenueReportResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CourierRepository courierRepository;

    public ReportService(OrderRepository orderRepository,
                         PaymentRepository paymentRepository,
                         CourierRepository courierRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.courierRepository = courierRepository;
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(Instant from, Instant to) {
        if (from == null) from = Instant.now().minusSeconds(86400 * 30); // 30 days
        if (to == null) to = Instant.now();

        List<Order> orders = orderRepository.findByCreatedAtBetween(from, to);

        long totalOrders = orders.size();
        long completedOrders = 0;
        long cancelledOrders = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalDiscounts = BigDecimal.ZERO;
        BigDecimal totalDeliveryFees = BigDecimal.ZERO;

        Map<String, Long> categoryItemCounts = new HashMap<>();
        Map<String, BigDecimal> categoryRevenues = new HashMap<>();

        for (Order o : orders) {
            if (o.getStatus() == OrderStatus.COMPLETED) {
                completedOrders++;
                totalRevenue = totalRevenue.add(o.getTotalAmount());
                totalDiscounts = totalDiscounts.add(o.getDiscountAmount());
                totalDeliveryFees = totalDeliveryFees.add(o.getDeliveryFee());

                if (o.getItems() != null) {
                    for (OrderItem item : o.getItems()) {
                        String catName = (item.getProduct() != null && item.getProduct().getCategory() != null)
                                ? item.getProduct().getCategory().getName()
                                : "Другое";
                        categoryItemCounts.merge(catName, (long) item.getQuantity(), Long::sum);
                        categoryRevenues.merge(catName, item.getLineTotal(), BigDecimal::add);
                    }
                }
            } else if (o.getStatus() == OrderStatus.CANCELLED) {
                cancelledOrders++;
            }
        }

        BigDecimal averageCheck = (completedOrders > 0)
                ? totalRevenue.divide(BigDecimal.valueOf(completedOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<CategorySalesDto> categoryList = new ArrayList<>();
        for (String catName : categoryItemCounts.keySet()) {
            long count = categoryItemCounts.get(catName);
            BigDecimal catRev = categoryRevenues.getOrDefault(catName, BigDecimal.ZERO);
            BigDecimal share = (totalRevenue.compareTo(BigDecimal.ZERO) > 0)
                    ? catRev.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            categoryList.add(new CategorySalesDto(catName, count, catRev, share));
        }

        RevenueReportResponse report = new RevenueReportResponse();
        report.setPeriodStart(from);
        report.setPeriodEnd(to);
        report.setTotalOrdersCount(totalOrders);
        report.setCompletedOrdersCount(completedOrders);
        report.setCancelledOrdersCount(cancelledOrders);
        report.setTotalRevenue(totalRevenue);
        report.setAverageCheck(averageCheck);
        report.setTotalDiscounts(totalDiscounts);
        report.setTotalDeliveryFees(totalDeliveryFees);
        report.setCategoryBreakdown(categoryList);

        return report;
    }

    // Stage 6: Apache POI Excel Report Generation (.xlsx)
    @Transactional(readOnly = true)
    public byte[] generateSalesExcelReport(Instant from, Instant to) throws IOException {
        RevenueReportResponse data = getRevenueReport(from, to);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0.00 ₽"));

            // Sheet 1: Sales Summary
            Sheet sheet1 = workbook.createSheet("Сводка продаж");
            sheet1.setColumnWidth(0, 8000);
            sheet1.setColumnWidth(1, 6000);

            Row r0 = sheet1.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("PizzaFlow - Финансовый отчёт по продажам");
            c0.setCellStyle(titleStyle);

            Row r1 = sheet1.createRow(1);
            r1.createCell(0).setCellValue("Период с: " + DateTimeFormatter.ISO_INSTANT.format(data.getPeriodStart()) + " по: " + DateTimeFormatter.ISO_INSTANT.format(data.getPeriodEnd()));

            String[][] metrics = {
                    {"Всего создано заказов", String.valueOf(data.getTotalOrdersCount())},
                    {"Успешно доставлено / выдано", String.valueOf(data.getCompletedOrdersCount())},
                    {"Отменено заказов", String.valueOf(data.getCancelledOrdersCount())},
                    {"Итоговая выручка ресторана", data.getTotalRevenue().toString() + " ₽"},
                    {"Средний чек завершённого заказа", data.getAverageCheck().toString() + " ₽"},
                    {"Сумма предоставленных скидок", data.getTotalDiscounts().toString() + " ₽"},
                    {"Сумма сборов за доставку", data.getTotalDeliveryFees().toString() + " ₽"}
            };

            int rowIdx = 3;
            Row headerRow1 = sheet1.createRow(rowIdx++);
            Cell h1 = headerRow1.createCell(0);
            h1.setCellValue("Показатель KPI");
            h1.setCellStyle(headerStyle);
            Cell h2 = headerRow1.createCell(1);
            h2.setCellValue("Значение");
            h2.setCellStyle(headerStyle);

            for (String[] metric : metrics) {
                Row row = sheet1.createRow(rowIdx++);
                Cell kCell = row.createCell(0);
                kCell.setCellValue(metric[0]);
                Cell vCell = row.createCell(1);
                vCell.setCellValue(metric[1]);
            }

            // Sheet 2: Category Breakdown
            Sheet sheet2 = workbook.createSheet("Продажи по категориям");
            sheet2.setColumnWidth(0, 6000);
            sheet2.setColumnWidth(1, 4000);
            sheet2.setColumnWidth(2, 5000);
            sheet2.setColumnWidth(3, 4000);

            Row catHeader = sheet2.createRow(0);
            String[] catHeaders = {"Категория", "Продано единиц", "Выручка", "Доля в выручке (%)"};
            for (int i = 0; i < catHeaders.length; i++) {
                Cell hc = catHeader.createCell(i);
                hc.setCellValue(catHeaders[i]);
                hc.setCellStyle(headerStyle);
            }

            int catRowIdx = 1;
            for (CategorySalesDto cat : data.getCategoryBreakdown()) {
                Row row = sheet2.createRow(catRowIdx++);
                row.createCell(0).setCellValue(cat.getCategoryName());
                row.createCell(1).setCellValue(cat.getItemsSold());
                Cell revCell = row.createCell(2);
                revCell.setCellValue(cat.getRevenue().doubleValue());
                revCell.setCellStyle(currencyStyle);
                row.createCell(3).setCellValue(cat.getPercentageShare().doubleValue() + "%");
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // Stage 6: Apache POI Word Report Generation (.docx)
    @Transactional(readOnly = true)
    public byte[] generateShiftWordReport(LocalDate shiftDate) throws IOException {
        if (shiftDate == null) shiftDate = LocalDate.now();

        Instant startOfDay = shiftDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = shiftDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        RevenueReportResponse reportData = getRevenueReport(startOfDay, endOfDay);
        List<Courier> couriers = courierRepository.findAll();

        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Title
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("PIZZAFLOW - ОТЧЁТ ПО СМЕНЕ");
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setColor("B22222"); // Firebrick Red

            // Subtitle
            XWPFParagraph subPara = document.createParagraph();
            subPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun subRun = subPara.createRun();
            subRun.setText("Дата смены: " + shiftDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            subRun.setFontSize(12);
            subRun.setItalic(true);

            // Section 1: KPI
            XWPFParagraph sec1 = document.createParagraph();
            XWPFRun sec1Run = sec1.createRun();
            sec1Run.setText("1. Ключевые показатели работы ресторана");
            sec1Run.setBold(true);
            sec1Run.setFontSize(14);

            XWPFTable kpiTable = document.createTable();
            kpiTable.setWidth("100%");

            XWPFTableRow kpiHeader = kpiTable.getRow(0);
            kpiHeader.getCell(0).setText("Метрика");
            kpiHeader.addNewTableCell().setText("Значение за смену");

            String[][] kpiValues = {
                    {"Выручка за смену", reportData.getTotalRevenue() + " ₽"},
                    {"Выполнено заказов", String.valueOf(reportData.getCompletedOrdersCount())},
                    {"Средний чек", reportData.getAverageCheck() + " ₽"},
                    {"Предоставлено скидок", reportData.getTotalDiscounts() + " ₽"},
                    {"Собрано за доставку", reportData.getTotalDeliveryFees() + " ₽"},
                    {"Отменено заказов", String.valueOf(reportData.getCancelledOrdersCount())}
            };

            for (String[] pair : kpiValues) {
                XWPFTableRow r = kpiTable.createRow();
                r.getCell(0).setText(pair[0]);
                r.getCell(1).setText(pair[1]);
            }

            // Section 2: Couriers
            XWPFParagraph sec2 = document.createParagraph();
            XWPFRun sec2Run = sec2.createRun();
            sec2Run.setText("2. Активность курьерской службы");
            sec2Run.setBold(true);
            sec2Run.setFontSize(14);

            XWPFTable courierTable = document.createTable();
            XWPFTableRow cHeader = courierTable.getRow(0);
            cHeader.getCell(0).setText("Курьер");
            cHeader.addNewTableCell().setText("Транспорт");
            cHeader.addNewTableCell().setText("Текущий статус");

            for (Courier c : couriers) {
                XWPFTableRow r = courierTable.createRow();
                r.getCell(0).setText(c.getUser() != null ? c.getUser().getFullName() : "Курьер #" + c.getId());
                r.getCell(1).setText(c.getVehicleType().name());
                r.getCell(2).setText(c.isAvailable() ? "Свободен (в очереди)" : "На заказе");
            }

            // Section 3: Summary note
            XWPFParagraph notePara = document.createParagraph();
            notePara.setSpacingBefore(300);
            XWPFRun noteRun = notePara.createRun();
            noteRun.setText("Отчёт сгенерирован автоматически системой PizzaFlow. Данные проверены и заверены управляющим смены.");
            noteRun.setItalic(true);
            noteRun.setFontSize(10);

            document.write(out);
            return out.toByteArray();
        }
    }
}
