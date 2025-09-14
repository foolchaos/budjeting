package com.example.budjeting.ui.references;

import com.example.budjeting.model.BudgetArticle;
import com.example.budjeting.repository.BudgetArticleRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.TreeGrid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;

/**
 * CRUD view for hierarchical Budget Articles.
 */
public class BudgetArticleView extends VerticalLayout {
    private final BudgetArticleRepository repo;
    private final TreeGrid<BudgetArticle> grid;

    public BudgetArticleView(BudgetArticleRepository repo) {
        this.repo = repo;
        setSizeFull();
        grid = new TreeGrid<>(BudgetArticle.class, false);
        grid.addHierarchyColumn(BudgetArticle::getCode).setHeader("Код");
        grid.addColumn(BudgetArticle::getName).setHeader("Наименование");
        refresh();

        Button add = new Button("Создать", e -> openForm(new BudgetArticle()));
        Button edit = new Button("Редактировать", e -> {
            BudgetArticle selected = grid.asSingleSelect().getValue();
            if (selected != null) {
                openForm(selected);
            }
        });
        Button delete = new Button("Удалить", e -> {
            BudgetArticle selected = grid.asSingleSelect().getValue();
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
        List<BudgetArticle> roots = repo.findAll().stream().filter(a -> a.getParent() == null).toList();
        grid.setItems(roots, BudgetArticle::getChildren);
    }

    private void openForm(BudgetArticle article) {
        Dialog dialog = new Dialog();
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<BudgetArticle> parent = new ComboBox<>("Родитель");
        parent.setItems(repo.findAll());
        parent.setItemLabelGenerator(BudgetArticle::getName);
        Button save = new Button("Сохранить", ev -> {
            article.setCode(code.getValue());
            article.setName(name.getValue());
            article.setParent(parent.getValue());
            repo.save(article);
            dialog.close();
            refresh();
        });
        VerticalLayout layout = new VerticalLayout(code, name, parent, save);
        dialog.add(layout);
        if (article.getId() != null) {
            code.setValue(article.getCode());
            name.setValue(article.getName());
            parent.setValue(article.getParent());
        }
        dialog.open();
    }
}
