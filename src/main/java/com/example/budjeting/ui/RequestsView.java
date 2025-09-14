package com.example.budjeting.ui;

import com.example.budjeting.model.AppRequest;
import com.example.budjeting.repository.AppRequestRepository;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "requests", layout = MainView.class)
public class RequestsView extends VerticalLayout {
    public RequestsView(AppRequestRepository repository) {
        Grid<AppRequest> grid = new Grid<>(AppRequest.class, false);
        grid.addColumn(AppRequest::getNumber).setHeader("Номер");
        grid.setItems(repository.findAll());
        add(grid);
    }
}
