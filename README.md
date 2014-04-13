DHD
===

This project attempts to solve dynamic hierarchical decomposition of networks.
Given an optimal hierarchical decomposition of a network, the goal is to find an optimal decomposition of the graph that results from edge perturbations.
Ideally, the new decomposition should be found without applying an ILP to the entire graph.

Provided is implementation of HIDEN and my dynamic approach.

See run and meta_script for example commands. 
run demonstrates how to use HIDEN on large graphs as well as showing the mutate function and the dynamic approach. 
meta_script supplies a simple testing frameworks (including graph generation) for comparing HIDEN and the dynamic approach under different situations.
