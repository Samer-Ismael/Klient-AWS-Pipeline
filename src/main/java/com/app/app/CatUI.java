package com.app.app;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@Route("")
public class CatUI extends VerticalLayout {

    private final String backendUrl = "http://samer.eu-north-1.elasticbeanstalk.com";  // Replace with your actual backend URL

    private final Grid<Cat> catGrid = new Grid<>(Cat.class);
    private final TextField name = new TextField("Name");
    private final TextField color = new TextField("Color");
    private final TextField age = new TextField("Age");

    public CatUI() {
        catGrid.setColumns("id", "name", "color", "age");

        Image image = new Image(new StreamResource("cat.jpg", () -> getClass().getResourceAsStream("/images/cat.jpg")), "Cat Image");
        image.getElement().getStyle().set("width", "250px");
        image.getElement().getStyle().set("height", "250px");

        HorizontalLayout imageLayout = new HorizontalLayout(image);
        imageLayout.setAlignItems(Alignment.START);

        HorizontalLayout inputLayout = new HorizontalLayout(name, color, age);
        inputLayout.setSpacing(true);

        Button addCatButton = new Button("Add Cat");
        Button updateCatButton = new Button("Update Cat");
        Button deleteCatButton = new Button("Delete Cat");
        Button swaggerButton = new Button("Swagger Documentation");
        HorizontalLayout buttonLayout = new HorizontalLayout(addCatButton, updateCatButton, deleteCatButton, swaggerButton);
        buttonLayout.setSpacing(true);

        addCatButton.addClickListener(e -> addCat());
        updateCatButton.addClickListener(e -> updateCat());
        deleteCatButton.addClickListener(e -> deleteCat());
        swaggerButton.addClickListener(e -> navigateToSwaggerUi());

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        add(imageLayout, inputLayout, buttonLayout, catGrid);

        if (!isServerAvailable()) {
            showServerDownNotification();
        } else {
            refreshGrid();
        }
    }

    private void navigateToSwaggerUi() {
        UI.getCurrent().getPage().setLocation(backendUrl + "/swagger-ui/index.html");
    }

    private void addCat() {
        try {
            String catName = name.getValue();
            String catColor = color.getValue();
            String catAge = age.getValue();

            Cat cat = new Cat();
            cat.setName(catName);
            cat.setColor(catColor);
            cat.setAge(Integer.parseInt(catAge));

            ResponseEntity<Void> responseEntity = new RestTemplate().postForEntity(
                    backendUrl + "/cat/",
                    cat,
                    Void.class
            );

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                refreshGrid();
                Notification.show("Cat added successfully", 3000, Notification.Position.TOP_CENTER);
            } else {
                Notification.show("Failed to add cat: " + responseEntity.getStatusCode(), 3000, Notification.Position.TOP_CENTER);
            }
        } catch (NumberFormatException e) {
            Notification.show("Something went wrong.", 3000, Notification.Position.TOP_CENTER);
        }
    }

    private void updateCat() {
        Cat selectedCat = catGrid.asSingleSelect().getValue();
        if (selectedCat != null) {
            try {
                selectedCat.setName(name.getValue());
                selectedCat.setColor(color.getValue());
                selectedCat.setAge(Integer.parseInt(age.getValue()));

                ResponseEntity<Void> responseEntity = new RestTemplate().exchange(
                        backendUrl + "/cat/" + selectedCat.getId(),
                        HttpMethod.PUT,
                        new HttpEntity<>(selectedCat),
                        Void.class
                );

                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    refreshGrid();
                    Notification.show("Cat updated successfully", 3000, Notification.Position.TOP_CENTER);
                } else {
                    Notification.show("Failed to update cat: " + responseEntity.getStatusCode(), 3000, Notification.Position.TOP_CENTER);
                }
            } catch (NumberFormatException e) {
                Notification.show("Something went wrong.", 3000, Notification.Position.TOP_CENTER);
            }
        } else {
            Notification.show("Select a cat to update", 3000, Notification.Position.TOP_CENTER);
        }
    }

    private void deleteCat() {
        Cat selectedCat = catGrid.asSingleSelect().getValue();
        if (selectedCat != null) {
            ResponseEntity<Void> responseEntity = new RestTemplate().exchange(
                    backendUrl + "/cat/" + selectedCat.getId(),
                    HttpMethod.DELETE,
                    null,
                    Void.class
            );

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                refreshGrid();
                Notification.show("Cat deleted successfully", 3000, Notification.Position.TOP_CENTER);
            } else {
                Notification.show("Failed to delete cat: " + responseEntity.getStatusCode(), 3000, Notification.Position.TOP_CENTER);
            }
        } else {
            Notification.show("Select a cat to delete", 3000, Notification.Position.TOP_CENTER);
        }
    }

    private void refreshGrid() {
        ResponseEntity<List<Cat>> responseEntity = new RestTemplate().exchange(
                backendUrl + "/cat/all",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Cat>>() {}
        );

        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            List<Cat> cats = responseEntity.getBody();
            catGrid.setItems(cats);
            catGrid.deselectAll();
        } else {
            Notification.show("Error fetching cat data: " + responseEntity.getStatusCode());
        }
    }


    private boolean isServerAvailable() {
        try {
            // Replace "/health" with the actual endpoint you want to check
            URL url = new URL(backendUrl + "/cat/all");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Set a timeout for the connection (e.g., 5 seconds)
            connection.setConnectTimeout(5000);

            int responseCode = connection.getResponseCode();

            // If the response code is in the range 200-299, it means the server is available
            return responseCode >= 200 && responseCode < 300;
        } catch (Exception e) {
            // Log the exception if needed
            return false; // Server is not available
        }
    }

    private void showServerDownNotification() {
        Notification.show("Server is down. Please try again later.", 0, Notification.Position.MIDDLE);
    }
}

