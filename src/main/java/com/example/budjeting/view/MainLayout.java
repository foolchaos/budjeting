package com.example.budjeting.view;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.component.html.Span;

public class MainLayout extends AppLayout {

    public MainLayout() {
        H1 title = new H1("Budjeting");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)");
        title.getStyle().set("margin", "0");
        addToNavbar(new DrawerToggle(), title);

        VerticalLayout menu = new VerticalLayout();
        menu.add(new RouterLink("Статьи БДЗ", BdzItemView.class));
        menu.add(new RouterLink("Статьи БО", BoArticleView.class));
        menu.add(new RouterLink("ЦФО/МВЗ", CfoMvzView.class));
        menu.add(new RouterLink("Курирующие ЗГД", SupervisorView.class));
        menu.add(new RouterLink("Договоры", ContractView.class));
        menu.add(new RouterLink("Заявки", ApplicationView.class));
        addToDrawer(menu);
    }
}
