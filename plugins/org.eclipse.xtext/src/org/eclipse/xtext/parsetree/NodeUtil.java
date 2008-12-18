/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.parsetree;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * @author Sven Efftinge - Initial contribution and API
 * @author Heiko Behrens
 * 
 */
public class NodeUtil {
	
	private static final Logger logger = Logger.getLogger(NodeUtil.class);
	
    public static NodeAdapter getNodeAdapter(EObject obj) {
    	if (obj == null)
    		return null;
        return (NodeAdapter) EcoreUtil.getAdapter(obj.eAdapters(), AbstractNode.class);
    }

    protected static boolean removeNodeAdapter(EObject obj) {
        NodeAdapter adapter = getNodeAdapter(obj);
        if (adapter == null)
            return false;
        while (adapter != null) {
            adapter.getParserNode().setParent(null);
            adapter = getNodeAdapter(obj);
        }
        return true;
    }

    public static CompositeNode getRootNode(EObject obj) {
        NodeAdapter adapter = getNodeAdapter(obj);
        if (adapter == null)
            return null;
        CompositeNode parserNode = adapter.getParserNode();
        if (parserNode == null)
            return null;
        return (CompositeNode) EcoreUtil.getRootContainer(parserNode);
    }

    public static List<CompositeNode> getCompositeChildren(CompositeNode parent) {
        List<CompositeNode> compositeChildren = new ArrayList<CompositeNode>();
        for (AbstractNode node : parent.getChildren()) {
            if (node instanceof CompositeNode) {
                compositeChildren.add((CompositeNode) node);
            }
        }
        return compositeChildren;
    }
    
    public static EObject getASTElementForRootNode(CompositeNode compositeNode) {
    	if(compositeNode.getElement() != null) {
    		return compositeNode.getElement();
    	}
    	WHILE: while (!compositeNode.getChildren().isEmpty()) {
			boolean foundCompositeChild = false;
			for (AbstractNode child : compositeNode.getChildren()) {
				if (child instanceof CompositeNode) {
					if (foundCompositeChild) {
						throw new IllegalStateException(
								"Corrupt node model: Composite node without element has multiple composite children");
					}
					foundCompositeChild = true;
					CompositeNode childComposite = (CompositeNode) child;
					if (childComposite.getElement() != null) {
						return childComposite.getElement();
					}
					else {
						compositeNode = childComposite;
						continue WHILE;
					}
				}
			}
			return null;
		}
    	return null;
    }
    
    public static EObject findASTParentElement(CompositeNode replaceRootNode) {
		CompositeNode parent = replaceRootNode.getParent();
		if (parent == null) {
			return null;
		}
		if (parent.getElement() != null) {
			return parent.getElement();
		}
		return findASTParentElement(parent);
	}

    public static void dumpCompositeNodes(String indent, CompositeNode node) {
    	if (logger.isTraceEnabled()) {
	    	dumpCompositeNodeInfo(indent, node);
	        for (AbstractNode childNode : node.getChildren()) {
	            if (childNode instanceof CompositeNode) {
	                CompositeNode compositeNode = (CompositeNode) childNode;
	                dumpCompositeNodes(indent + "  ", compositeNode);
	            }
	        }
    	}
    }
    
    public static void dumpCompositeNodeInfo(String indent, CompositeNode node) {
    	if (logger.isTraceEnabled()) {
			EObject grammarElement = node.getGrammarElement();
			String name;
			try {
				name = grammarElement.getClass().getMethod("getName").invoke(grammarElement).toString();
			}
			catch (Exception exc) {
				name = grammarElement.getClass().getSimpleName();
			}
			String astElementAsString = (node.getElement() == null) ? "null" : node.getElement().eClass().getName();
			String line = indent + name + " : " + node.serialize() + " -> " + astElementAsString + "  la={ ";
			for (LeafNode lookaheadNode : node.getLookaheadLeafNodes()) {
				line += "\"" + lookaheadNode.getText() + "\" ";
			}
			logger.trace("}   (" + node.getTotalOffset() + ", " + node.getTotalLength() + ")");
		}
    }
    
	public static AbstractNode findLeafNodeAtOffset(CompositeNode parseTreeRootNode, int offset) {
		for (AbstractNode node : parseTreeRootNode.getChildren()) {
			if (node.getTotalOffset() + node.getTotalLength() >= offset) {
				if (node.getTotalOffset() <= offset) {
					if (node instanceof LeafNode)
						return (LeafNode) node;
					else if (node instanceof CompositeNode)
						return findLeafNodeAtOffset((CompositeNode) node, offset);
				}
			}
		}
		return null;
	}
	
	public static CompositeNode findLastCompositeGrandChild(CompositeNode parentNode) {
		EList<AbstractNode> children = parentNode.getChildren();
		for(int i=children.size()-1; i>=0; --i) {
			AbstractNode child = children.get(i);
			if (child instanceof CompositeNode) {
				return findLastCompositeGrandChild((CompositeNode) child);
			}
		}
		return parentNode;
	}
	
	public static void checkOffsetConsistency(CompositeNode rootNode) {
		int currentOffset = rootNode.getTotalOffset();
		for(AbstractNode child:rootNode.getChildren()) {
			if(child.getTotalOffset() != currentOffset) {
				throw new IllegalStateException("Invalid offset: Should be " + currentOffset + " but is " + child.getTotalOffset() + "\n" + child.serialize());
			}
			if(child instanceof CompositeNode) {
				checkOffsetConsistency((CompositeNode) child);
			}
			int serializedLength = child.serialize().length();
			if(child.getTotalLength() != serializedLength) {
				throw new IllegalStateException("Invalid length: Should be " + serializedLength + " but is " + child.getTotalLength() + "\n" + child.serialize());	
			}
			currentOffset += serializedLength;
		}
	}

	public static EObject getNearestSemanticObject(AbstractNode node) {
		while(node != null) {
			if(node.getElement()!=null)
				return node.getElement();
			
			node = (AbstractNode) node.eContainer();
		}	

		return null;
	}

}
