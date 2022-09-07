# fst (Finite State Transducer)

This code uses the algorithm "[Minimal Acyclic Subsequential Transducers](http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.24.3698&rep=rep1&type=pdf)".

This project is for learning purpose.

about FST construction : Minimal Acyclic Subsequential Transducers, relevant code is in FstBuilder <br><br>
about how nodes and transitions are represented in an FST:<br>
Experiments with Automata Compression<br>
Smaller Representation of Finite State Automata<br>
, relevant code is in FstWriter.
<br>

about Levenshtein Automata: https://julesjacobs.com/2015/06/17/disqus-levenshtein-simple-and-fast.html <br>
, relevant code is mainly in another repository named LevenshteinAutomaton.

API: FstMap, FstSet

reference:<br>
https://blog.burntsushi.net/transducers/ <br>
https://github.com/yhirose/cpp-fstlib <br>