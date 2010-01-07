package net.sourceforge.pmd.eclipse.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.eclipse.ui.preferences.br.CellPainterBuilder;
import net.sourceforge.pmd.eclipse.ui.preferences.br.RuleFieldAccessor;
import net.sourceforge.pmd.util.ClassUtil;
import net.sourceforge.pmd.util.StringUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class Util {

    public static final Object[] EMPTY_ARRAY = new Object[0];
    
    public static final Comparator<Method> MethodNameComparator = new Comparator<Method>() {
        public int compare(Method a, Method b) {            
            return a.getName().compareTo(b.getName());
        }
    };
    
    private Util() {};
    
    /**
     * Scans the source for occurrences of the prefix char and assumes that all letters
     * that follow immediately after make up a reference name. Returns the position and
     * lengths of all names found.
     * 
     * NOTE: Doesn't handle the prefix within escaped or quoted strings
     * 
     * @param source
     * @param prefix
     * @return List<int[]>
     */
    public static List<int[]> referencedNamePositionsIn(String source, char prefix) {
        
        List<int[]> namePositions = new ArrayList<int[]>();
        if (StringUtil.isEmpty(source)) return namePositions;
        
        int pos = source.indexOf(prefix);
        int max = source.length();
        
        while (pos >= 0) {            
            int end = pos+1;
            while (end < max && Character.isLetter(source.charAt(end)) ) end++;
            int length = end - pos -1;
            if (length < 1) continue;
            
            namePositions.add( new int[] {pos+1, length} );
            pos = source.indexOf(prefix, pos + length);
        }
        
        return namePositions;
    }
    
    /**
     * Extract and return all the string fragments listed in the list of positions
     * where each position = int [start][length]
     * 
     * @param source
     * @param positions
     * @return List<String>
     */
    public static List<String> fragmentsWithin(String source, List<int[]> positions) {
        
        List<String> fragments = new ArrayList<String>(positions.size());
        for (int[] position : positions) {
            fragments.add(
                    source.substring(position[0], position[0] + position[1])
                    );
        }
        return fragments;
    }
    
    public static String signatureFor(Method method, String[] unwantedPrefixes) {

        StringBuilder sb = new StringBuilder();
        Class<?> returnType = method.getReturnType();
        if (returnType.getName().equals("void")) {  // TODO is there a better way?
            sb.append("void ");
        } else {
            signatureFor(returnType, unwantedPrefixes, sb);
            sb.append(' ');
        }
        Class<?>[] types = method.getParameterTypes();
        if (types.length == 0) {
            sb.append(method.getName());
            sb.append("()");
            return sb.toString();
        }
        
        sb.append(method.getName()).append('(');
        signatureFor(types[0], unwantedPrefixes, sb);
        for (int i=1; i<types.length; i++) {
            sb.append(',');
            signatureFor(types[i], unwantedPrefixes, sb);
        }
        sb.append(')');
        return sb.toString();
    }
  
    private static String filteredPrefixFrom(String paramType, String[] unwantedPrefixes) {
        for (String prefix : unwantedPrefixes) {
            if (paramType.startsWith(prefix)) {
                return paramType.substring(prefix.length());
            }
        }
        return paramType;
    }
    
    public static String signatureFor(Class<?> type, String[] unwantedPrefixes) {
        
        StringBuilder sb = new StringBuilder();
        signatureFor(type, unwantedPrefixes, sb);
        return sb.toString();
    }
    
    private static void signatureFor(Class<?> type, String[] unwantedPrefixes, StringBuilder sb) {        
        
        String typeName = ClassUtil.asShortestName(
                type.isArray() ? type.getComponentType() : type
                );
        typeName = filteredPrefixFrom(typeName, unwantedPrefixes);
        
        sb.append(typeName);
        if (type.isArray()) sb.append("[]");
    }
    
	public static Comparator<?> comparatorFrom(final RuleFieldAccessor accessor, final boolean inverted) {
		
		return new Comparator() {

			public int compare(Object a, Object b) {
				Comparable ca = accessor.valueFor((Rule) a);
				Comparable cb = accessor.valueFor((Rule) b);
				
				int result = (ca == null) ? 
						-1 : (cb == null) ? 
							1 : ca.compareTo(cb);
				
				return inverted ? result * -1 : result;
			}			
		};
	}
	
    public static void removeListeners(Control widget, int listenerType) {
        for (Listener listener : widget.getListeners(listenerType)) {
            widget.removeListener(listenerType, listener);
        }
    }	
    
    public static int indexOf(Object[] items, Object choice) {
        for (int i=0; i<items.length; i++) {
            if (items[i].equals(choice)) return i;
        }
        return -1;
    }
    
	/**
	 * Method asCleanString.
	 * @param original String
	 * @return String
	 */
	public static String asCleanString(String original) {
		return original == null ? "" : original.trim();
	}
	
	public static enum shape { square, circle };
	
	public static CellPainterBuilder textAsColorShapeFor(final int width, final int height, final shape shapeId) {
	    
	   
	    return new CellPainterBuilder() {

	        private ColourManager colorManager;
	        
	        private ColourManager colorManagerFor(Display display) {
	            if (colorManager != null) return colorManager;
	            colorManager = ColourManager.managerFor(display);
	            return colorManager;
	        }
	        
	        private String getterTextIn(TreeItem tItem, RuleFieldAccessor getter) {
	            
	            Object item = tItem.getData();
	            if (!(item instanceof Rule)) return null;
	            String text = (String)getter.valueFor((Rule) item);
                return StringUtil.isEmpty(text) ? null : text;
	        }
	        	        
            public void addPainterFor(final Tree tree, final int columnIndex, final RuleFieldAccessor getter, Map<Integer, List<Listener>> listenersByEventCode) {
                
                Listener paintListener = new Listener() {
                    public void handleEvent(Event event) {
                        if (event.index != columnIndex) return;
                        
                        String text = getterTextIn((TreeItem)event.item, getter);
                        if (text == null) return;
                        
                        Color original = event.gc.getBackground();
                        
                        Color clr = colorManagerFor(event.display).colourFor(text);
                        event.gc.setBackground(clr);
                        
                        switch (shapeId) {
	                        case square: {
	                        	event.gc.fillRectangle(event.x+1, event.y+2, width, height);    // fill it
	                        	event.gc.drawRectangle(event.x+1, event.y+2, width, height);    // then the border on top
	                        	break;
	                        	}
	                        case circle: {
	                        	event.gc.fillArc(event.x+1, event.y+2, width, height, 0, 360*64);    // fill it
	                        	event.gc.drawArc(event.x+1, event.y+2, width, height, 0, 360*64);    // then the border on top
	                        	}
                        }
                        
                        event.gc.setBackground(original);
                    }
                };
                                   
                Listener measureListener = new Listener() {
                    public void handleEvent(Event e) {
                        if (e.index != columnIndex) return;
                        e.width = width + 2;
                        e.height = height + 2;
                    }
                };
                
                addListener(tree, SWT.PaintItem, paintListener, listenersByEventCode);
                addListener(tree, SWT.MeasureItem, measureListener, listenersByEventCode);
            }            
	    };
	}
	
    private static void addListener(Control control, int eventType, Listener listener, Map<Integer, List<Listener>> listenersByEventCode) {
        
        Integer eventCode = Integer.valueOf(eventType);
        
        control.addListener(eventType, listener);
        if (!listenersByEventCode.containsKey(eventCode)) {
            listenersByEventCode.put(eventCode, new ArrayList<Listener>());
            }
        
        listenersByEventCode.get(eventCode).add(listener);
    }
    
	public static CellPainterBuilder backgroundBuilderFor(final int systemColourIndex) {
	    
	    return new CellPainterBuilder() {

	        public void addPainterFor(final Tree tree, final int columnIndex, final RuleFieldAccessor getter, Map<Integer, List<Listener>> paintListeners) {
	            
	            final Display display = tree.getDisplay();
	            
	            tree.addListener(SWT.EraseItem, new Listener() {
	                public void handleEvent(Event event) {
	                    
	                    if (event.index != columnIndex) return;
	                    event.detail &= ~SWT.HOT;
	                    
	                    if ((event.detail & SWT.SELECTED) != 0) {
	                        GC gc = event.gc;
	                        Rectangle area = tree.getClientArea();
	                        /*
	                         * If you wish to paint the selection beyond the end of last column, you must change the clipping region.
	                         */
	                        int columnCount = tree.getColumnCount();
	                        if (event.index == columnCount - 1 || columnCount == 0) {
	                            int width = area.x + area.width - event.x;
	                            if (width > 0) {
	                                Region region = new Region();
	                                gc.getClipping(region);
	                                region.add(event.x, event.y, width, event.height); 
	                                gc.setClipping(region);
	                                region.dispose();
	                            }
	                        }
	                        gc.setAdvanced(true);
	                        if (gc.getAdvanced()) gc.setAlpha(127);                             
	                        Rectangle rect = event.getBounds();
	                        Color foreground = gc.getForeground();
	                        Color background = gc.getBackground();
	                        gc.setForeground(display.getSystemColor(systemColourIndex));
	                        gc.setBackground(display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
	                        gc.fillGradientRectangle(event.x, rect.y, 500, rect.height, false);
	                        
	                        gc.setForeground(display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
	                        gc.drawLine(event.x, rect.y, event.x + 20, rect.y + 20);
	                        
	                        gc.setForeground(foreground);  // restore colors for subsequent drawing
	                        gc.setBackground(background);
	                        event.detail &= ~SWT.SELECTED;                  
	                    }
	                }       
	            });
	        }};
	}
	
	   public static CellPainterBuilder textBuilderFor(final int systemColourIndex) {
	        
	        return new CellPainterBuilder() {

	            public void addPainterFor(final Tree tree, final int columnIndex, final RuleFieldAccessor getter, Map<Integer, List<Listener>> paintListeners) {
	                
	                final Display display = tree.getDisplay();

//	                   tree.addListener(SWT.EraseItem, new Listener() {
//	                        public void handleEvent(Event event) {
//	                            if (event.index != columnIndex) return;
//	                            
//	                //            GC gc = event.gc;
//                   //             Rectangle area = new Rectangle(event.x, event.y, event.width, event.height);
//                               
//       //                         gc.fillRectangle(area);
//	                        }
//	         
//	                        });
	                
	                tree.addListener(SWT.PaintItem, new Listener() {
	                    public void handleEvent(Event event) {
	                        
	                        if (event.index != columnIndex) return;
	                        event.detail &= ~SWT.HOT;
	                        
//	                        if ((event.detail & SWT.SELECTED) != 0) {
	                            GC gc = event.gc;
	                            Rectangle area = tree.getClientArea();
	                            
	                            Rule rule = (Rule)((TreeItem)event.item).getData();
	                            String text = getter.valueFor(rule).toString();
	                            
	                            int columnCount = tree.getColumnCount();
	                            if (event.index == columnCount - 1 || columnCount == 0) {
	                                int width = area.x + area.width - event.x;
	                                if (width > 0) {
	                                    Region region = new Region();
	                                    gc.getClipping(region);
	                                    region.add(event.x, event.y, width, event.height); 
	                                    gc.setClipping(region);
	                                    region.dispose();
	                                }
	                            }
	                        //    gc.setAdvanced(true);
	                        //    if (gc.getAdvanced()) gc.setAlpha(127);                             
	                            Rectangle rect = event.getBounds();
	                     //       Color foreground = gc.getForeground();
	                     //       Color background = gc.getBackground();
	                     //       gc.setForeground(display.getSystemColor(systemColourIndex));
	                     //       gc.setBackground(display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
	                            
	                     //       gc.setForeground(display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
	                            gc.drawString(text, event.x, rect.y);
	                            
	                     //       gc.setForeground(foreground);  // restore colors for subsequent drawing
	                     //       gc.setBackground(background);
//	                            event.detail &= ~SWT.SELECTED;                  
//	                        }
	                    }       
	                });
	            }};
	    }
	
	
	// TODO move these to StringUtil
    public static String asString(List<String> items, String separator) {
        
        if (items == null || items.isEmpty()) return "";
        if (items.size() == 1) return items.get(0);
        
        StringBuilder sb = new StringBuilder(items.get(0));
        for (int i=1; i<items.size(); i++) {
            sb.append(separator).append(items.get(i));
        }
        return sb.toString();
    }
    
    public static void asString(Object[] items, String separator, StringBuilder target) {
        
        if (items == null || items.length==0) return;
         
        target.append(items[0]);
        for (int i=1; i<items.length; i++) {
            target.append(separator).append(items[i]);
        }
    }
}