package com.example.budjeting.ui.references;

import com.example.budjeting.model.BOArticle;
import com.example.budjeting.model.BudgetArticle;
import com.example.budjeting.repository.BOArticleRepository;
import com.example.budjeting.repository.BudgetArticleRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

/**
 * CRUD view for BO Articles.
 */
public class BOArticleView extends VerticalLayout {
    private final BOArticleRepository repo;
    private final BudgetArticleRepository budgetRepo;
    private final Grid<BOArticle> grid;

    public BOArticleView(BOArticleRepository repo, BudgetArticleRepository budgetRepo) {
        this.repo = repo;
        this.budgetRepo = budgetRepo;
        setSizeFull();
        grid = new Grid<>(BOArticle.class, false);
        grid.addColumn(BOArticle::getCode).setHeader("Код");
        grid.addColumn(BOArticle::getName).setHeader("Наименование");
        grid.addColumn(a -> {
            BudgetArticle b = a.getBudgetArticle();
            return b != null ? b.getName() : "";
        }).setHeader("Статья БДЗ");
        refresh();

        Button add = new Button("Создать", e -> openForm(new BOArticle()));
        Button edit = new Button("Редактировать", e -> {
            BOArticle selected = grid.asSingleSelect().getValue();
            if (selected != null) {
                openForm(selected);
            }
        });
        Button delete = new Button("Удалить", e -> {
            BOArticle selected = grid.asSingleSelect().getValue();
            if (selected != null) {
                repo.delete(selected);
                refresh();
            }
        });
        HorizontalLayout actions = new HorizontalLayout(add, edit, delete);
        add(actions, grid);
        setFlexGrow(1, grid);
    }

    private void refresh() {
        grid.setItems(repo.findAll());
    }

    private void openForm(BOArticle item) {
        Dialog dialog = new Dialog();
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<BudgetArticle> budget = new ComboBox<>("Статья БДЗ");
        budget.setItems(budgetRepo.findAll());
        budget.setItemLabelGenerator(BudgetArticle::getName);
        Button save = new Button("Сохранить", ev -> {
            item.setCode(code.getValue());
            item.setName(name.getValue());
            item.setBudgetArticle(budget.getValue());
            repo.save(item);
            dialog.close();
            refresh();
        });
        VerticalLayout layout = new VerticalLayout(code, name, budget, save);
        dialog.add(layout);
        if (item.getId() != null) {
            code.setValue(item.getCode());
            name.setValue(item.getName());
            budget.setValue(item.getBudgetArticle());
        }
        dialog.open();
    }
}
