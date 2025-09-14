package com.example.budjeting.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "requests", layout = MainLayout.class)
public class RequestsView extends VerticalLayout {
    public RequestsView() {
        add(new H1("\u0417\u0430\u044f\u0432\u043a\u0438"));
    }
}
