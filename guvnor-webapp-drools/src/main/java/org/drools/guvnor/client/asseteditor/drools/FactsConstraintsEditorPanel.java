/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.guvnor.client.asseteditor.drools;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;

import org.drools.guvnor.client.common.FormStylePopup;
import org.drools.guvnor.client.common.ImageButton;
import org.drools.guvnor.client.common.SmallLabel;
import org.drools.guvnor.client.messages.Constants;
import org.drools.guvnor.client.moduleeditor.drools.SuggestionCompletionCache;
import org.drools.guvnor.client.resources.DroolsGuvnorImages;
import org.drools.guvnor.client.rpc.Asset;
import org.drools.guvnor.client.rpc.WorkingSetConfigData;
import org.drools.ide.common.client.factconstraints.ConstraintConfiguration;
import org.drools.ide.common.client.factconstraints.helper.ConstraintsContainer;
import org.drools.ide.common.client.modeldriven.SuggestionCompletionEngine;

import java.util.*;

public class FactsConstraintsEditorPanel extends Composite {


    private static int idGenerator = 0;
    private ListBox factsCombo = new ListBox(false);
    private ListBox fieldsCombo = new ListBox(false);
    private ListBox constraintsCombo = new ListBox(false);
    private VerticalPanel vpConstraintConf = new VerticalPanel();
    private boolean validFactsChanged = true;
    private Map<String, ConstraintConfiguration> contraintsMap = new HashMap<String, ConstraintConfiguration>();
    private final Asset workingSet;
    private final WorkingSetEditor workingSetEditor;

    public FactsConstraintsEditorPanel(WorkingSetEditor workingSetEditor) {

        this.workingSetEditor = workingSetEditor;

        this.workingSet = workingSetEditor.getWorkingSet();

        factsCombo.setVisibleItemCount(1);
        fieldsCombo.setVisibleItemCount(1);
        constraintsCombo.setVisibleItemCount(5);

        factsCombo.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                fillSelectedFactFields();
            }
        });

        fieldsCombo.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                fillFieldConstrains();
            }
        });

        Image addNewConstraint = new ImageButton(DroolsGuvnorImages.INSTANCE.newItem());
        addNewConstraint.setTitle(Constants.INSTANCE.AddNewConstraint());

        addNewConstraint.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                showNewConstrainPop();
            }
        });

        Image removeConstraint = new Image(DroolsGuvnorImages.INSTANCE.trash());
        removeConstraint.setTitle(Constants.INSTANCE.removeConstraint());
        removeConstraint.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                removeConstraint();
            }
        });

        final FlexTable table = new FlexTable();

        VerticalPanel vp = new VerticalPanel();
        vp.add(new SmallLabel(Constants.INSTANCE.FactTypes()));
        vp.add(factsCombo);
        table.setWidget(0,
                0,
                vp);

        vp = new VerticalPanel();
        vp.add(new SmallLabel(Constants.INSTANCE.Field()));
        vp.add(fieldsCombo);
        table.setWidget(1,
                0,
                vp);

        vp = new VerticalPanel();
        HorizontalPanel hp = new HorizontalPanel();
        vp.add(new SmallLabel(Constants.INSTANCE.Constraints()));
        hp.add(constraintsCombo);

        VerticalPanel btnPanel = new VerticalPanel();
        btnPanel.add(addNewConstraint);
        btnPanel.add(removeConstraint);
        hp.add(btnPanel);
        vp.add(hp);
        table.setWidget(2,
                0,
                vp);
        table.getFlexCellFormatter().setRowSpan(2,
                0,
                3);
        constraintsCombo.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                showConstraintConfig();
            }
        });

        vpConstraintConf.add(new SmallLabel(Constants.INSTANCE.ConstraintsParameters()));
        vpConstraintConf.add(new SmallLabel(""));
        table.setWidget(0,
                1,
                vpConstraintConf);
        table.getFlexCellFormatter().setRowSpan(0,
                1,
                5);

        fillSelectedFacts();
        fillSelectedFactFields();
        fillFieldConstrains();
        showConstraintConfig();

        this.initWidget(table);
    }

    protected final void fillSelectedFacts() {
        if (validFactsChanged) {
            String s = factsCombo.getSelectedIndex() != -1 ? factsCombo.getItemText(factsCombo.getSelectedIndex()) : "";
            factsCombo.clear();
            validFactsChanged = false;
            for (int i = 0; i < workingSetEditor.getValidFactsListBox().getItemCount(); i++) {
                String itemText = workingSetEditor.getValidFactsListBox().getItemText(i);
                factsCombo.addItem(itemText);
                if (s.equals(itemText)) {
                    factsCombo.setSelectedIndex(i);
                }
            }
            if (factsCombo.getSelectedIndex() == -1 && factsCombo.getItemCount() > 0) {
                factsCombo.setSelectedIndex(0);
            }
            fillSelectedFactFields();
        }
    }

    private void fillSelectedFactFields() {
        if (factsCombo.getSelectedIndex() != -1) {
            String fact = factsCombo.getItemText(factsCombo.getSelectedIndex());
            fieldsCombo.clear();
            for (String field : getCompletionEngine().getFieldCompletions(fact)) {
                fieldsCombo.addItem(field);
            }
        }
        if (fieldsCombo.getSelectedIndex() == -1 && fieldsCombo.getItemCount() > 0) {
            fieldsCombo.setSelectedIndex(0);
        }
        fillFieldConstrains();
    }

    private void fillFieldConstrains() {
        if (fieldsCombo.getSelectedIndex() != -1 && factsCombo.getSelectedIndex() != -1) {
            String fieldName = fieldsCombo.getItemText(fieldsCombo.getSelectedIndex());

            String factField = factsCombo.getItemText(factsCombo.getSelectedIndex());
            constraintsCombo.clear();
            contraintsMap.clear();
            for (ConstraintConfiguration c : this.workingSetEditor.getConstraintsConstrainer().getConstraints(factField,
                    fieldName)) {
                constraintsCombo.addItem(c.getConstraintName(),
                        addContrainsMap(c));
            }
            vpConstraintConf.remove(vpConstraintConf.getWidgetCount() - 1);
            vpConstraintConf.add(new SmallLabel());
        }
        showConstraintConfig();
    }

    synchronized private String addContrainsMap(ConstraintConfiguration c) {
        String constraintId = String.valueOf(idGenerator++);
        contraintsMap.put(constraintId,
                c);
        return constraintId;
    }

    protected void removeConstraint() {
        if ( constraintsCombo.getSelectedIndex() != -1 ) {
            ConstraintConfiguration c = contraintsMap.get( constraintsCombo.getValue( constraintsCombo.getSelectedIndex() ) );
            ((WorkingSetConfigData) workingSet.getContent()).constraints = this.workingSetEditor.getConstraintsConstrainer().removeConstraint( c );
        }
        fillFieldConstrains();
    }

    private void showConstraintConfig() {
        if (constraintsCombo.getItemCount() == 0) {
            vpConstraintConf.remove(vpConstraintConf.getWidgetCount() - 1);
            vpConstraintConf.add(new SmallLabel());
            return;
        }
        if (constraintsCombo.getSelectedIndex() != -1) {
            ConstraintConfiguration c = contraintsMap.get(constraintsCombo.getValue(constraintsCombo.getSelectedIndex()));
            ConstraintEditor editor = new ConstraintEditor(c);
            vpConstraintConf.remove(vpConstraintConf.getWidgetCount() - 1);
            vpConstraintConf.add(editor);
        }
    }

    private void showNewConstrainPop() {
        final FormStylePopup pop = new FormStylePopup(
                DroolsGuvnorImages.INSTANCE.config(),
                Constants.INSTANCE.AddNewConstraint());
        final Button addbutton = new Button(Constants.INSTANCE.OK());
        final ListBox consDefsCombo = new ListBox(false);

        consDefsCombo.setVisibleItemCount(5);

        addbutton.setTitle(Constants.INSTANCE.AddNewConstraint());

        List<String> names = new ArrayList<String>(ConstraintsContainer.getAllConfigurations().keySet());
        Collections.sort(names);
        for (String name : names) {
            consDefsCombo.addItem(name);
        }

        addbutton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                String name = consDefsCombo.getItemText(consDefsCombo.getSelectedIndex());
                ConstraintConfiguration config = ConstraintsContainer.getEmptyConfiguration(name);
                if (config != null) {

                    String factName = factsCombo.getItemText( factsCombo.getSelectedIndex() );
                    String fieldName = fieldsCombo.getItemText( fieldsCombo.getSelectedIndex() );
                    config.setFactType( factName );
                    config.setFieldName( fieldName );
                    if ( ((WorkingSetConfigData) workingSet.getContent()).constraints == null ) {
                        ((WorkingSetConfigData) workingSet.getContent()).constraints = new ArrayList<ConstraintConfiguration>();
                    }
                    ((WorkingSetConfigData) workingSet.getContent()).constraints.add( config );
                    constraintsCombo.addItem( config.getConstraintName(),
                                              addContrainsMap( config ) );
                    workingSetEditor.getConstraintsConstrainer().addConstraint( config );

                }
                pop.hide();
            }
        });

        pop.addAttribute(
                Constants.INSTANCE.WillExtendTheFollowingRuleCalled(),
                consDefsCombo);
        pop.addAttribute("",
                addbutton);

        pop.show();
    }

    private SuggestionCompletionEngine getCompletionEngine() {
        return SuggestionCompletionCache.getInstance().getEngineFromCache( workingSet.getMetaData().getModuleName() );
    }

    public void notifyValidFactsChanged() {
        this.validFactsChanged = true;
    }
}
