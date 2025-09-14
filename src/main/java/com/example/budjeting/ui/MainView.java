package com.example.budjeting.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;

@Route("")
public class MainView extends AppLayout {

    private final Span directories = new Span("Справочники");
    private final Span requests = new Span("Заявки");

    public MainView() {
        Tab dirTab = new Tab("Справочники");
        Tab reqTab = new Tab("Заявки");
        Tabs tabs = new Tabs(dirTab, reqTab);
        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab() == dirTab) {
                setContent(directories);
            } else {
                setContent(requests);
            }
        });
        addToNavbar(tabs);
        setContent(directories);
    }
}
