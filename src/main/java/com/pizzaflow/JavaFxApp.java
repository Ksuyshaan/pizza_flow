package com.pizzaflow;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApp extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(PizzaflowApplication.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("PizzaFlow");
        stage.setScene(new Scene(new Label("PizzaFlow запущен"), 600, 400));
        stage.show();
    }

    @Override
    public void stop() {
        context.close();
    }
}