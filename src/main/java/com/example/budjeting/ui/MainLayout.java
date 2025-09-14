package com.example.budjeting.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;

/**
 * Main application layout containing top level navigation tabs.
 */
public class MainLayout extends AppLayout {
    public MainLayout() {
        Tabs tabs = new Tabs();
        tabs.add(createTab("Справочники", ReferencesView.class),
                 createTab("Заявки", RequestsView.class));
        addToNavbar(tabs);
    }

    private Tab createTab(String label, Class<? extends Component> navigationTarget) {
        RouterLink link = new RouterLink(label, navigationTarget);
        link.setTabIndex(-1);
        return new Tab(link);
    }
}
