/*
 * This file is part of lanterna (http://code.google.com/p/lanterna/).
 * 
 * lanterna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright (C) 2010-2016 Martin
 */
package com.googlecode.lanterna.gui2;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;

import java.util.List;

/**
 * Simple layout manager the puts all components on a single line, either horizontally or vertically.
 */
public class LinearLayout implements LayoutManager {
    /**
     * This enum type will decide the alignment of a component on the counter-axis, meaning the horizontal alignment on
     * vertical {@code LinearLayout}s and vertical alignment on horizontal {@code LinearLayout}s.
     */
    public enum Alignment {
        /**
         * The component will be placed to the left (for vertical layouts) or top (for horizontal layouts)
         */
        Beginning,
        /**
         * The component will be placed horizontally centered (for vertical layouts) or vertically centered (for
         * horizontal layouts)
         */
        Center,
        /**
         * The component will be placed to the right (for vertical layouts) or bottom (for horizontal layouts)
         */
        End,
        /**
         * The component will be forced to take up all the horizontal space (for vertical layouts) or vertical space
         * (for horizontal layouts)
         */
        Fill,
    }

    private static class LinearLayoutData implements LayoutData {
        private final Alignment alignment;

        public LinearLayoutData(Alignment alignment) {
            this.alignment = alignment;
        }
    }

    /**
     * Creates a {@code LayoutData} for {@code LinearLayout} that assigns a component to a particular alignment on its
     * counter-axis, meaning the horizontal alignment on vertical {@code LinearLayout}s and vertical alignment on
     * horizontal {@code LinearLayout}s.
     * @param alignment Alignment to store in the {@code LayoutData} object
     * @return {@code LayoutData} object created for {@code LinearLayout}s with the specified alignment
     * @see Alignment
     */
    public static LayoutData createLayoutData(Alignment alignment) {
        return new LinearLayoutData(alignment);
    }

    private final Direction direction;
    private int spacing;
    private boolean changed;

    /**
     * Default constructor, creates a vertical {@code LinearLayout}
     */
    public LinearLayout() {
        this(Direction.VERTICAL);
    }

    /**
     * Standard constructor that creates a {@code LinearLayout} with a specified direction to position the components on
     * @param direction Direction for this {@code Direction}
     */
    public LinearLayout(Direction direction) {
        this.direction = direction;
        this.spacing = direction == Direction.HORIZONTAL ? 1 : 0;
        this.changed = true;
    }

    /**
     * Sets the amount of empty space to put in between components. For horizontal layouts, this is number of columns
     * (by default 1) and for vertical layouts this is number of rows (by default 0).
     * @param spacing Spacing between components, either in number of columns or rows depending on the direction
     * @return Itself
     */
    public LinearLayout setSpacing(int spacing) {
        this.spacing = spacing;
        this.changed = true;
        return this;
    }

    /**
     * Returns the amount of empty space to put in between components. For horizontal layouts, this is number of columns
     * (by default 1) and for vertical layouts this is number of rows (by default 0).
     * @return Spacing between components, either in number of columns or rows depending on the direction
     */
    public int getSpacing() {
        return spacing;
    }

    @Override
    public TerminalSize getPreferredSize(List<Component> components) {
        if(direction == Direction.VERTICAL) {
            return getPreferredSizeVertically(components);
        }
        else {
            return getPreferredSizeHorizontally(components);
        }
    }

    private TerminalSize getPreferredSizeVertically(List<Component> components) {
        int maxWidth = 0;
        int height = 0;
        for(Component component: components) {
            TerminalSize preferredSize = component.getPreferredSize();
            if(maxWidth < preferredSize.getColumns()) {
                maxWidth = preferredSize.getColumns();
            }
            height += preferredSize.getRows();
        }
        height += spacing * (components.size() - 1);
        return new TerminalSize(maxWidth, height);
    }

    private TerminalSize getPreferredSizeHorizontally(List<Component> components) {
        int maxHeight = 0;
        int width = 0;
        for(Component component: components) {
            TerminalSize preferredSize = component.getPreferredSize();
            if(maxHeight < preferredSize.getRows()) {
                maxHeight = preferredSize.getRows();
            }
            width += preferredSize.getColumns();
        }
        width += spacing * (components.size() - 1);
        return new TerminalSize(width, maxHeight);
    }

    @Override
    public boolean hasChanged() {
        return changed;
    }

    @Override
    public void doLayout(TerminalSize area, List<Component> components) {
        if(direction == Direction.VERTICAL) {
            doVerticalLayout(area, components);
        }
        else {
            doHorizontalLayout(area, components);
        }
        this.changed = false;
    }

    private void doVerticalLayout(TerminalSize area, List<Component> components) {
        int remainingVerticalSpace = area.getRows();
        int availableHorizontalSpace = area.getColumns();
        for(Component component: components) {
            if(remainingVerticalSpace <= 0) {
                component.setPosition(TerminalPosition.TOP_LEFT_CORNER);
                component.setSize(TerminalSize.ZERO);
            }
            else {
                LinearLayoutData layoutData = (LinearLayoutData)component.getLayoutData();
                Alignment alignment = Alignment.Beginning;
                if(layoutData != null) {
                    alignment = layoutData.alignment;
                }

                TerminalSize preferredSize = component.getPreferredSize();
                TerminalSize decidedSize = new TerminalSize(
                        Math.min(availableHorizontalSpace, preferredSize.getColumns()),
                        Math.min(remainingVerticalSpace, preferredSize.getRows()));
                if(alignment == Alignment.Fill) {
                    decidedSize = decidedSize.withColumns(availableHorizontalSpace);
                    alignment = Alignment.Beginning;
                }

                TerminalPosition position = component.getPosition();
                position = position.withRow(area.getRows() - remainingVerticalSpace);
                switch(alignment) {
                    case End:
                        position = position.withColumn(availableHorizontalSpace - decidedSize.getColumns());
                        break;
                    case Center:
                        position = position.withColumn((availableHorizontalSpace - decidedSize.getColumns()) / 2);
                        break;
                    case Beginning:
                    default:
                        position = position.withColumn(0);
                        break;
                }
                component.setPosition(position);
                component.setSize(component.getSize().with(decidedSize));
                remainingVerticalSpace -= decidedSize.getRows() + spacing;
            }
        }
    }

    private void doHorizontalLayout(TerminalSize area, List<Component> components) {
        int remainingHorizontalSpace = area.getColumns();
        int availableVerticalSpace = area.getRows();
        for(Component component: components) {
            if(remainingHorizontalSpace <= 0) {
                component.setPosition(TerminalPosition.TOP_LEFT_CORNER);
                component.setSize(TerminalSize.ZERO);
            }
            else {
                LinearLayoutData layoutData = (LinearLayoutData)component.getLayoutData();
                Alignment alignment = Alignment.Beginning;
                if(layoutData != null) {
                    alignment = layoutData.alignment;
                }
                TerminalSize preferredSize = component.getPreferredSize();
                TerminalSize decidedSize = new TerminalSize(
                        Math.min(remainingHorizontalSpace, preferredSize.getColumns()),
                        Math.min(availableVerticalSpace, preferredSize.getRows()));
                if(alignment == Alignment.Fill) {
                    decidedSize = decidedSize.withRows(availableVerticalSpace);
                    alignment = Alignment.Beginning;
                }

                TerminalPosition position = component.getPosition();
                position = position.withColumn(area.getColumns() - remainingHorizontalSpace);
                switch(alignment) {
                    case End:
                        position = position.withRow(availableVerticalSpace - decidedSize.getRows());
                        break;
                    case Center:
                        position = position.withRow((availableVerticalSpace - decidedSize.getRows()) / 2);
                        break;
                    case Beginning:
                    default:
                        position = position.withRow(0);
                        break;
                }
                component.setPosition(position);
                component.setSize(component.getSize().with(decidedSize));
                remainingHorizontalSpace -= decidedSize.getColumns() + spacing;
            }
        }
    }
}
