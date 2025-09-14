package com.example.budjeting.view;

import com.example.budjeting.entity.BoArticle;
import com.example.budjeting.entity.BdzItem;
import com.example.budjeting.repository.BdzItemRepository;
import com.example.budjeting.repository.BoArticleRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "bo", layout = MainLayout.class)
@PageTitle("Статьи БО")
public class BoArticleView extends VerticalLayout {

    private final BoArticleRepository repository;
    private final BdzItemRepository bdzItemRepository;
    private final Grid<BoArticle> grid = new Grid<>(BoArticle.class);
    private final Binder<BoArticle> binder = new Binder<>(BoArticle.class);
    private BoArticle current;

    public BoArticleView(@Autowired BoArticleRepository repository, @Autowired BdzItemRepository bdzItemRepository) {
        this.repository = repository;
        this.bdzItemRepository = bdzItemRepository;
        configureGrid();
        add(grid, createForm());
        updateList();
    }

    private void configureGrid() {
        grid.setColumns("code", "name");
        grid.addColumn(item -> item.getBdzItem() != null ? item.getBdzItem().getName() : "").setHeader("Статья БДЗ");
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));
    }

    private FormLayout createForm() {
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        ComboBox<BdzItem> bdzCombo = new ComboBox<>("Статья БДЗ");
        bdzCombo.setItems(bdzItemRepository.findAll());
        bdzCombo.setItemLabelGenerator(BdzItem::getName);

        binder.bind(code, BoArticle::getCode, BoArticle::setCode);
        binder.bind(name, BoArticle::getName, BoArticle::setName);
        binder.bind(bdzCombo, BoArticle::getBdzItem, BoArticle::setBdzItem);

        Button save = new Button("Сохранить", e -> save());
        Button add = new Button("Добавить", e -> edit(new BoArticle()));
        Button delete = new Button("Удалить", e -> delete());
        HorizontalLayout actions = new HorizontalLayout(save, add, delete);
        return new FormLayout(code, name, bdzCombo, actions);
    }

    private void edit(BoArticle item) {
        current = item;
        binder.setBean(item);
    }

    private void save() {
        repository.save(current);
        updateList();
    }

    private void delete() {
        if (current != null && current.getId() != null) {
            repository.delete(current);
            updateList();
        }
    }

    private void updateList() {
        grid.setItems(repository.findAll());
    }
}
