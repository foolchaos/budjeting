package com.example.budget.ui;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.orderedlayout.FlexLayout;

@Route("")
public class MainView extends VerticalLayout {

    private final Tab refsTab = new Tab("Справочники");
    private final Tab reqsTab = new Tab("Заявки");
    private final Tabs tabs = new Tabs(refsTab, reqsTab);

    private final ReferencesView referencesView;
    private final RequestsView requestsView;

    public MainView(ReferencesView referencesView, RequestsView requestsView) {
        this.referencesView = referencesView;
        this.requestsView = requestsView;

        setSizeFull();
        add(new H1("Бюджет: доходы и затраты"));
        tabs.setWidthFull();
        add(tabs);

        add(referencesView, requestsView);
        referencesView.setVisible(true);
        requestsView.setVisible(false);

        tabs.addSelectedChangeListener(e -> {
            boolean refs = e.getSelectedTab() == refsTab;
            referencesView.setVisible(refs);
            requestsView.setVisible(!refs);
        });
        expand(referencesView, requestsView);
    }
}
