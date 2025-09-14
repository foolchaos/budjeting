package com.example.budjeting.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {
    public MainLayout() {
        Tabs tabs = new Tabs();
        tabs.add(createTab("Справочники", DirectoriesView.class));
        tabs.add(createTab("Заявки", RequestsView.class));
        addToNavbar(tabs);
    }

    private Tab createTab(String label, Class<?> navigationTarget) {
        return new Tab(new RouterLink(label, navigationTarget));
    }
}
