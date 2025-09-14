package com.example.budjeting.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Справочники")
public class DirectoryView extends SplitLayout {
    public DirectoryView() {
        Tabs menu = new Tabs();
        menu.add(new Tab("БДЗ"), new Tab("БО"), new Tab("ЗГД"), new Tab("ЦФО"), new Tab("МВЗ"), new Tab("Договор"));
        setSizeFull();
        addToPrimary(menu);
        Div placeholder = new Div();
        placeholder.setText("Выберите справочник");
        addToSecondary(placeholder);
    }
}
