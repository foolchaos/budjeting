package com.example.budjeting.ui;

import com.example.budjeting.domain.Request;
import com.example.budjeting.service.RequestService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "requests", layout = MainLayout.class)
@PageTitle("Заявки")
public class RequestView extends VerticalLayout {
    public RequestView(RequestService service) {
        Grid<Request> grid = new Grid<>(Request.class);
        grid.setItems(service.findAll());
        add(grid);
    }
}
