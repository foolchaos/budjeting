package com.example.budjeting.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("")
public class MainView extends AppLayout {
    public MainView() {
        Tabs tabs = new Tabs();
        tabs.add(createTab("Справочники", ReferencesView.class),
                 createTab("Заявки", RequestsView.class));
        addToNavbar(tabs);
    }

    private Tab createTab(String label, Class<?> navigationTarget) {
        RouterLink link = new RouterLink(label, navigationTarget);
        link.setTabIndex(-1);
        return new Tab(link);
    }
}
