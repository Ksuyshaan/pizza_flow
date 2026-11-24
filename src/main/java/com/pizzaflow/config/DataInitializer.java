package com.pizzaflow.config;

import com.pizzaflow.delivery.entity.Courier;
import com.pizzaflow.delivery.entity.DeliveryZone;
import com.pizzaflow.delivery.entity.VehicleType;
import com.pizzaflow.delivery.repository.CourierRepository;
import com.pizzaflow.delivery.repository.DeliveryZoneRepository;
import com.pizzaflow.kitchen.entity.*;
import com.pizzaflow.kitchen.repository.CategoryRepository;
import com.pizzaflow.kitchen.repository.IngredientRepository;
import com.pizzaflow.kitchen.repository.ProductRepository;
import com.pizzaflow.kitchen.repository.RecipeItemRepository;
import com.pizzaflow.order.entity.DiscountType;
import com.pizzaflow.order.entity.PromoCode;
import com.pizzaflow.order.entity.User;
import com.pizzaflow.order.entity.UserRole;
import com.pizzaflow.order.repository.PromoCodeRepository;
import com.pizzaflow.order.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEFAULT_PASS = "password123";

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final ProductRepository productRepository;
    private final RecipeItemRepository recipeItemRepository;
    private final DeliveryZoneRepository deliveryZoneRepository;
    private final CourierRepository courierRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           IngredientRepository ingredientRepository,
                           ProductRepository productRepository,
                           RecipeItemRepository recipeItemRepository,
                           DeliveryZoneRepository deliveryZoneRepository,
                           CourierRepository courierRepository,
                           PromoCodeRepository promoCodeRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.productRepository = productRepository;
        this.recipeItemRepository = recipeItemRepository;
        this.deliveryZoneRepository = deliveryZoneRepository;
        this.courierRepository = courierRepository;
        this.promoCodeRepository = promoCodeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("DataInitializer: database already seeded, skipping.");
            return;
        }

        log.info("=== DataInitializer: seeding demo data ===");

        // ── Users ──────────────────────────────────────────────────────────────
        String hash = passwordEncoder.encode(DEFAULT_PASS);
        User customer  = userRepository.save(new User("customer@pizzaflow.com",  hash, "Иван Петров",    "+7-900-111-0001", UserRole.CUSTOMER));
        User cook      = userRepository.save(new User("cook@pizzaflow.com",       hash, "Сергей Готовцев", "+7-900-111-0002", UserRole.COOK));
        User courier1  = userRepository.save(new User("courier@pizzaflow.com",    hash, "Андрей Быстров",  "+7-900-111-0003", UserRole.COURIER));
        User courier2  = userRepository.save(new User("courier2@pizzaflow.com",   hash, "Мария Скорова",   "+7-900-111-0004", UserRole.COURIER));
        User manager   = userRepository.save(new User("manager@pizzaflow.com",    hash, "Ольга Управлова", "+7-900-111-0005", UserRole.MANAGER));
        User admin     = userRepository.save(new User("admin@pizzaflow.com",      hash, "Алексей Главный", "+7-900-111-0006", UserRole.ADMIN));

        // ── Delivery Zones ──────────────────────────────────────────────────────
        DeliveryZone zone1 = deliveryZoneRepository.save(new DeliveryZone("Центр города",     new BigDecimal("150.00"), new BigDecimal("800.00"),  25));
        DeliveryZone zone2 = deliveryZoneRepository.save(new DeliveryZone("Спальный район",   new BigDecimal("250.00"), new BigDecimal("1200.00"), 40));
        DeliveryZone zone3 = deliveryZoneRepository.save(new DeliveryZone("Пригород",         new BigDecimal("400.00"), new BigDecimal("2000.00"), 60));

        // ── Couriers ──────────────────────────────────────────────────────────
       // courierRepository.save(new Courier(courier1, VehicleType.MOTO));
       // courierRepository.save(new Courier(courier2, VehicleType.BIKE));

        // ── Categories ─────────────────────────────────────────────────────────
        Category catPizza    = categoryRepository.save(new Category("Пицца",    "Классические и авторские пиццы на тонком и пышном тесте",        1));
        Category catDrinks   = categoryRepository.save(new Category("Напитки",  "Холодные и горячие напитки",                                      2));
        Category catDesserts = categoryRepository.save(new Category("Десерты",  "Сладкие завершения вашего обеда",                                 3));
        Category catSnacks   = categoryRepository.save(new Category("Закуски",  "Крылышки, хлебные палочки, брускетты",                            4));

        // ── Ingredients ────────────────────────────────────────────────────────
       // Ingredient flour       = ingredientRepository.save(new Ingredient("Мука пшеничная",        IngredientUnit.GRAM,   new BigDecimal("15000"), new BigDecimal("2000"), new BigDecimal("0.05")));
        Ingredient tomatoSauce = ingredientRepository.save(new Ingredient("Томатный соус",          IngredientUnit.ML,     new BigDecimal("8000"),  new BigDecimal("1000"), new BigDecimal("0.08")));
        //Ingredient mozzarella  = ingredientRepository.save(new Ingredient("Моцарелла",              IngredientUnit.GRAM,   new BigDecimal("5000"),  new BigDecimal("500"),  new BigDecimal("0.45")));
        //Ingredient pepperoni   = ingredientRepository.save(new Ingredient("Пепперони",              IngredientUnit.GRAM,   new BigDecimal("3000"),  new BigDecimal("300"),  new BigDecimal("0.80")));
        //Ingredient mushrooms   = ingredientRepository.save(new Ingredient("Шампиньоны свежие",      IngredientUnit.GRAM,   new BigDecimal("2000"),  new BigDecimal("300"),  new BigDecimal("0.25")));
        //Ingredient chicken     = ingredientRepository.save(new Ingredient("Куриное филе",           IngredientUnit.GRAM,   new BigDecimal("3000"),  new BigDecimal("500"),  new BigDecimal("0.35")));
        Ingredient bbqSauce    = ingredientRepository.save(new Ingredient("Соус BBQ",               IngredientUnit.ML,     new BigDecimal("2000"),  new BigDecimal("200"),  new BigDecimal("0.12")));
        //Ingredient olives      = ingredientRepository.save(new Ingredient("Маслины",                IngredientUnit.GRAM,   new BigDecimal("1000"),  new BigDecimal("100"),  new BigDecimal("0.60")));
        Ingredient cola        = ingredientRepository.save(new Ingredient("Coca-Cola (заготовка)",  IngredientUnit.ML,     new BigDecimal("20000"), new BigDecimal("2000"), new BigDecimal("0.03")));
        Ingredient water       = ingredientRepository.save(new Ingredient("Вода питьевая",          IngredientUnit.ML,     new BigDecimal("30000"), new BigDecimal("3000"), new BigDecimal("0.01")));
        //Ingredient sugar       = ingredientRepository.save(new Ingredient("Сахар",                  IngredientUnit.GRAM,   new BigDecimal("5000"),  new BigDecimal("500"),  new BigDecimal("0.04")));
        //Ingredient chocolate   = ingredientRepository.save(new Ingredient("Шоколад тёмный",         IngredientUnit.GRAM,   new BigDecimal("2000"),  new BigDecimal("300"),  new BigDecimal("0.90")));

        // ── Products ───────────────────────────────────────────────────────────
        // Pizzas
        Product margherita = productRepository.save(new Product(catPizza,  "Маргарита",         "Классика итальянской кухни: томатный соус и моцарелла",                new BigDecimal("490.00"), "🍕", 15));
        Product pepperoniP = productRepository.save(new Product(catPizza,  "Пепперони",         "Острая пепперони с томатным соусом и двойным слоем моцареллы",        new BigDecimal("620.00"), "🍕", 18));
        Product mushroomP  = productRepository.save(new Product(catPizza,  "Грибная",           "Шампиньоны, моцарелла, томатный соус — идеально для вегетарианцев", new BigDecimal("550.00"), "🍕", 16));
        Product chickenBBQ = productRepository.save(new Product(catPizza,  "Курица BBQ",        "Курица-гриль с соусом BBQ, моцарелла, маслины",                      new BigDecimal("680.00"), "🍕", 20));
        // Drinks
        Product colaD      = productRepository.save(new Product(catDrinks, "Coca-Cola 0.5 л",   "Классическая кола, сильногазированная",                               new BigDecimal("99.00"),  "🥤", 2));
        Product waterD     = productRepository.save(new Product(catDrinks, "Вода без газа 0.5 л","Минеральная вода без газа",                                           new BigDecimal("69.00"),  "💧", 1));
        // Desserts
        Product brownie    = productRepository.save(new Product(catDesserts,"Брауни шоколадный","Тёмный шоколад, хрустящая корочка, тающая середина",                  new BigDecimal("189.00"), "🍫", 5));
        // Snacks
        Product breadStick = productRepository.save(new Product(catSnacks,  "Хлебные палочки",  "5 хлебных палочек с чесночным соусом",                                new BigDecimal("149.00"), "🥖", 8));

        // ── Recipe Items ───────────────────────────────────────────────────────
        // Margherita
        //recipeItemRepository.save(new RecipeItem(margherita, flour,       new BigDecimal("200")));
        recipeItemRepository.save(new RecipeItem(margherita, tomatoSauce, new BigDecimal("80")));
        //recipeItemRepository.save(new RecipeItem(margherita, mozzarella,  new BigDecimal("150")));
        // Pepperoni
        //recipeItemRepository.save(new RecipeItem(pepperoniP, flour,       new BigDecimal("200")));
        recipeItemRepository.save(new RecipeItem(pepperoniP, tomatoSauce, new BigDecimal("80")));
        //recipeItemRepository.save(new RecipeItem(pepperoniP, mozzarella,  new BigDecimal("180")));
        //recipeItemRepository.save(new RecipeItem(pepperoniP, pepperoni,   new BigDecimal("120")));
        // Mushroom
        //recipeItemRepository.save(new RecipeItem(mushroomP,  flour,       new BigDecimal("200")));
        recipeItemRepository.save(new RecipeItem(mushroomP,  tomatoSauce, new BigDecimal("80")));
        //recipeItemRepository.save(new RecipeItem(mushroomP,  mozzarella,  new BigDecimal("150")));
        //recipeItemRepository.save(new RecipeItem(mushroomP,  mushrooms,   new BigDecimal("150")));
        // Chicken BBQ
        //recipeItemRepository.save(new RecipeItem(chickenBBQ, flour,       new BigDecimal("200")));
        recipeItemRepository.save(new RecipeItem(chickenBBQ, bbqSauce,    new BigDecimal("80")));
        //recipeItemRepository.save(new RecipeItem(chickenBBQ, mozzarella,  new BigDecimal("150")));
        //recipeItemRepository.save(new RecipeItem(chickenBBQ, chicken,     new BigDecimal("150")));
        //recipeItemRepository.save(new RecipeItem(chickenBBQ, olives,      new BigDecimal("40")));
        // Drinks
        recipeItemRepository.save(new RecipeItem(colaD,      cola,        new BigDecimal("500")));
        recipeItemRepository.save(new RecipeItem(waterD,     water,       new BigDecimal("500")));
        // Desserts
        //recipeItemRepository.save(new RecipeItem(brownie,    chocolate,   new BigDecimal("80")));
        //recipeItemRepository.save(new RecipeItem(brownie,    sugar,       new BigDecimal("50")));
        //recipeItemRepository.save(new RecipeItem(brownie,    flour,       new BigDecimal("60")));
        // Snacks
        //recipeItemRepository.save(new RecipeItem(breadStick, flour,       new BigDecimal("100")));

        // ── Promo Codes ─────────────────────────────────────────────────────────
        Instant now = Instant.now();
        Instant end = now.plus(365, ChronoUnit.DAYS);
        promoCodeRepository.save(new PromoCode("WELCOME10",  DiscountType.PERCENTAGE,  new BigDecimal("10"), new BigDecimal("0"),    null,                  100, now, end));
        promoCodeRepository.save(new PromoCode("PIZZA20",    DiscountType.PERCENTAGE,  new BigDecimal("20"), new BigDecimal("800"),  new BigDecimal("400"), 50,  now, end));
       // promoCodeRepository.save(new PromoCode("FIXED150",   DiscountType.FIXED_AMOUNT, new BigDecimal("150"), new BigDecimal("600"), null,                 200, now, end));

        log.info("=== DataInitializer: seeding complete ===");
        log.info("Demo accounts: customer@pizzaflow.com | cook@pizzaflow.com | courier@pizzaflow.com | manager@pizzaflow.com | admin@pizzaflow.com");
        log.info("All passwords: {}", DEFAULT_PASS);
    }
}
