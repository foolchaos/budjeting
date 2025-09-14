package com.example.budjeting.view;

import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class MainView extends VerticalLayout {
    public MainView() {
        Tab dictionaries = new Tab("Справочники");
        Tab requests = new Tab("Заявки");
        Tabs tabs = new Tabs(dictionaries, requests);
        add(tabs);
    }
}
