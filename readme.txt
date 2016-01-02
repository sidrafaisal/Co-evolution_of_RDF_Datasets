=================================================================================================================================
Co_Evolution_Manager:
=================================================================================================================================

main.java 
--------------
It calls configure for input settings. It calls strategy to be executed. Finally, it deletes the input files. 
Synchronized target is with the same name as initial target for future use.
  
configure.java
--------------
It configures input (filename,strategy etc) settings to be used by the application. It saves the settings in config.xml file.

strategy.java
--------------
This file implements different strategies. For conflict detection and resolution strategies, it calls the conflict finder.

=================================================================================================================================
Conflict_Finder:
=================================================================================================================================

conflicts_Finder.java
--------------
It implements conflict detection part. It calls source_Delta to identify conflicts. The target changesets are applied here. 

source_Delta.java
--------------
It applies the source changesets. Each triple from source changeset is checked in initial target and target changesets and their
pattern of Operation (e.g., source and target modification) is computed. If conflicting triples are detected, they are removed 
from corresponding files. It calls resolver in case of conflicting triples.

=================================================================================================================================
Conflict_Resolver:
=================================================================================================================================

manual_Selector.java
-------------
It creates/stores configuration (predicate,function) for future use. In case of resolution functions which, are triple independent 
(e.g. globalVote which returns frequently occurring value for a predicate), it calls the statistics. For such functions, data is 
computed at once. Each time a target creates manual_function_selection for predicate, it calls auto_selector to update the scores.

auto_Selector.java
-------------
It creates/stores configuration (predicate,function) for future use. In case of resolution functions which, are triple independent 
(e.g. globalVote which returns frequently occurring value for a predicate), it calls the statistics. For such functions, data is 
computed at once.    
In contrast to manual_selector, it implements auto-selection policy for selecting the resolution function. It will evolve with 
the passage of time. Whenever a manual selection is made, auto selector records the selection in form of scores. It keeps the score 
for each function against each predicate. For auto-selection, it will pick the function with highest score for the given predicate.

statistics.java
-------------
It implements the functions which compute data (e.g., dataset with less number of blanks for a certain predicate) before 
implementing the CDR strategies. The computed predicate-preferedDataset pairs are accessible through here. It also keeps 
a list of predicate-function pairs to be used by resolver.  

resolver.java
-------------
Depending on the selected function for conflict resolution, it calls F_Math or F_Generic.

F_Math.java
-------------
It calls F_Numbers or F_BigIntegers depending on input data type.

F_Numbers.java
-------------
It implements mathematical functions for data type int, float, double. 

F_BigIntegers.java
-------------
It implements mathematical functions for data type BigIntegers. 

F_Generic.java
-------------
It implements functions which are also applicable to more generic data types e.g., String.