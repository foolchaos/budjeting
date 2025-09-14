package com.example.budjeting.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {
    public MainLayout() {
        Tabs tabs = new Tabs();
        Tab dirTab = new Tab(new RouterLink("Справочники", DirectoryView.class));
        Tab reqTab = new Tab(new RouterLink("Заявки", RequestView.class));
        tabs.add(dirTab, reqTab);
        addToNavbar(tabs);
    }
}
