package com.example.budjeting.ui;

import com.example.budjeting.model.BudgetArticle;
import com.example.budjeting.repository.BudgetArticleRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route(value = "references", layout = MainView.class)
public class ReferencesView extends VerticalLayout {

    public ReferencesView(BudgetArticleRepository repository) {
        Grid<BudgetArticle> grid = new Grid<>(BudgetArticle.class, false);
        grid.addColumn(BudgetArticle::getCode).setHeader("Код");
        grid.addColumn(BudgetArticle::getName).setHeader("Наименование");
        grid.setItems(repository.findAll());

        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        Button add = new Button("Добавить", e -> {
            BudgetArticle ba = new BudgetArticle();
            ba.setCode(code.getValue());
            ba.setName(name.getValue());
            repository.save(ba);
            grid.setItems(repository.findAll());
            code.clear();
            name.clear();
        });

        add(grid, code, name, add);
    }
}
