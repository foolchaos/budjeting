package com.example.budjeting.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {
    public MainLayout() {
        Tab directories = new Tab(new RouterLink("\u0421\u043f\u0440\u0430\u0432\u043e\u0447\u043d\u0438\u043a\u0438", DirectoriesView.class));
        Tab requests = new Tab(new RouterLink("\u0417\u0430\u044f\u0432\u043a\u0438", RequestsView.class));
        Tabs tabs = new Tabs(directories, requests);
        addToNavbar(tabs);
    }
}
