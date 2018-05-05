/*
 * Copyright 2018, Robert Dyer, Mohd Arafat
 *                 and Bowling Green State University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package boa.graphs.pdg;

import boa.functions.BoaAstIntrinsics;
import boa.graphs.cdg.CDG;
import boa.graphs.cdg.CDGEdge;
import boa.graphs.cdg.CDGNode;
import boa.graphs.cfg.CFG;
import boa.graphs.ddg.DDG;
import boa.graphs.ddg.DDGNode;
import boa.types.Ast.*;
import boa.types.Control;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Program Dependence Graph builder
 *
 * @author marafat
 */

public class PDG {

    private Method md;
    private PDGNode entryNode;
    private HashSet<PDGNode> nodes = new HashSet<PDGNode>();

    // Constructors

    /**
     * Constructs a program dependence graph
     *
     * @param cdg control dependence graph
     * @param ddg data dependence graph
     */
    public PDG(final CDG cdg, final DDG ddg) {
        this.md = cdg.getMethod();
        if (cdg.getNodes().size() > 0) {
            addCDG(cdg);
            addDDGEdges(ddg);
            entryNode = getNode(0);
        }
    }

    /**
     * Constructs a program dependence graph.
     *
     * @param cfg control flow graph
     * @throws Exception if PDG construction fails
     */
    public PDG(final CFG cfg) throws Exception {
        this(new CDG(cfg), new DDG(cfg));
    }

    /**
     * Constructs a program dependence graph
     *
     * @param md method whose PDG is to be built
     * @param paramAsStatement if true, inserts parameters as assign statements at the
     *                         begining of control flow graph. Default is set to false
     * @throws Exception if PDG construction fails
     */
    public PDG(final Method md, boolean paramAsStatement) throws Exception {
        this(new CFG(md, paramAsStatement));
    }

    /**
     * Constructs a program dependence graph
     *
     * @param md method whose PDG is to be built
     * @throws Exception if PDG construction fails
     */
    public PDG(final Method md) throws Exception {
        this(new CFG(md, false));
    }

    // Getters

    /**
     * Returns the method whose PDG is built
     *
     * @return the method whose PDG is built
     */
    private Method getMethod() {
        return md;
    }

    /**
     * Returns the entry node to the graph
     *
     * @return the entry node to the graph
     */
    public PDGNode getEntryNode() {
        return entryNode;
    }

    /**
     * Returns the set of all the nodes in the graph
     *
     * @return the set of all the nodes in the graph
     */
    public HashSet<PDGNode> getNodes() {
        return nodes;
    }

    /**
     * Returns the PDG node for the given node id. If not found then returns null
     *
     * @param id node id
     * @return
     */
    public PDGNode getNode(int id) {
        for (PDGNode n: nodes)
            if (n.getId() == id)
                return n;

        return null;
    }

    /**
     * Adds CDG nodes and edges to program dependence graph
     *
     * @param cdg control dependence graph
     */
    private void addCDG(final CDG cdg) {
        for (CDGNode n: cdg.getNodes()) {
            PDGNode node = new PDGNode(n);
            nodes.add(node);
        }

        for (CDGNode n: cdg.getNodes()) {
            PDGNode node = getNode(n.getId());
            for (CDGNode s: n.getSuccessors())
                node.addSuccessor(getNode(s.getId()));
            for (CDGNode p: n.getPredecessors())
                node.addPredecessor(getNode(p.getId()));
            for (CDGEdge ie: n.getInEdges())
                node.addInEdge(new PDGEdge(getNode(ie.getSrc().getId()), getNode(ie.getDest().getId()), ie.getLabel(), Control.PDGEdge.PDGEdgeType.CONTROL));
            for (CDGEdge oe: n.getOutEdges())
                node.addOutEdge(new PDGEdge(getNode(oe.getSrc().getId()), getNode(oe.getDest().getId()), oe.getLabel(), Control.PDGEdge.PDGEdgeType.CONTROL));
        }

    }

    /**
     * Adds DDG edges to the PDG graph
     *
     * @param ddg data dependency graph
     */
    private void addDDGEdges(final DDG ddg) {
        try {
            for (Map.Entry<DDGNode, Set<DDGNode>> entry : ddg.getDefUseChain().entrySet()) {
                PDGNode src = getNode(entry.getKey().getId());
                for (DDGNode d : entry.getValue()) {
                    PDGNode dest = getNode(d.getId());
                    src.addSuccessor(dest);
                    dest.addPredecessor(src);
                    PDGEdge e = new PDGEdge(src, dest, entry.getKey().getDefVariable(), Control.PDGEdge.PDGEdgeType.DATA);
                    src.addOutEdge(e);
                    dest.addInEdge(e);
                }
            }
        } catch (Exception e) {
            System.out.println(BoaAstIntrinsics.prettyprint(md));
        }
    }

}