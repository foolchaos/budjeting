package com.example.budjeting.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
public class DirectoriesView extends VerticalLayout {
    public DirectoriesView() {
        add(new H1("\u0421\u043f\u0440\u0430\u0432\u043e\u0447\u043d\u0438\u043a\u0438"));
    }
}
