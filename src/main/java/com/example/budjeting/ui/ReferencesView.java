package com.example.budjeting.ui;

import com.example.budjeting.repository.*;
import com.example.budjeting.ui.references.*;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

/**
 * View containing all reference data management UIs.
 */
@Route(value = "", layout = MainLayout.class)
public class ReferencesView extends SplitLayout {
    private final Div content = new Div();

    public ReferencesView(BudgetArticleRepository budgetRepo,
                          BOArticleRepository boRepo,
                          SupervisorRepository supervisorRepo,
                          CFORepository cfoRepo,
                          MVZRepository mvzRepo,
                          ContractRepository contractRepo) {
        setSizeFull();
        ListBox<String> list = new ListBox<>();
        list.setItems("БДЗ", "БО", "ЗГД", "ЦФО", "МВЗ", "Договор");
        list.addValueChangeListener(e -> show(e.getValue(), budgetRepo, boRepo, supervisorRepo, cfoRepo, mvzRepo, contractRepo));
        content.setSizeFull();
        addToPrimary(list);
        addToSecondary(content);
        list.setValue("БДЗ");
        show("БДЗ", budgetRepo, boRepo, supervisorRepo, cfoRepo, mvzRepo, contractRepo);
    }

    private void show(String name,
                       BudgetArticleRepository budgetRepo,
                       BOArticleRepository boRepo,
                       SupervisorRepository supervisorRepo,
                       CFORepository cfoRepo,
                       MVZRepository mvzRepo,
                       ContractRepository contractRepo) {
        content.removeAll();
        switch (name) {
            case "БДЗ" -> content.add(new BudgetArticleView(budgetRepo));
            case "БО" -> content.add(new BOArticleView(boRepo, budgetRepo));
            case "ЗГД" -> content.add(new SupervisorView(supervisorRepo, budgetRepo));
            case "ЦФО" -> content.add(new CFOView(cfoRepo));
            case "МВЗ" -> content.add(new MVZView(mvzRepo, cfoRepo));
            case "Договор" -> content.add(new ContractView(contractRepo));
        }
    }
}
