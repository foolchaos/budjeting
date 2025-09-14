package com.example.budjeting.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {
    public MainLayout() {
        Tabs tabs = new Tabs();
        Tab catalogs = new Tab(new RouterLink("Справочники", CatalogsView.class));
        Tab requests = new Tab(new RouterLink("Заявки", RequestsView.class));
        tabs.add(catalogs, requests);
        addToNavbar(tabs);
    }
}
