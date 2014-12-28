LC-automatique
==============

This program uses Lending Club REST API to automate investment decisions, here you can specify multiple strategies where each strategy can have it's own filters and corresponding portfolio where loans should be added in your lending club account. It also allows you to schedule at what time do you want to run the program. For e.g. in-case you want to run everytime new loans are released by LC.

How to configure 
==============
Please specify your numeric investor id and API key in the configuration file. In configuration file you can specify multiple strategies where each strategy have it's own filters and you can decide loans ordered by the strategy would go in which corresponding portfolio in your lending club account. Please see the [sample configuraiton file](https://raw.githubusercontent.com/git-hemant/LC-automatique/master/examples/simple/simple.txt) which have one very simple strategy.


How to run 
==============
1) Download the [lc-automatique.jar](https://github.com/git-hemant/LC-automatique/blob/master/distrib/LC-automatique.jar?raw=true)
<br>
2) Run the program java -jar -Dconfig.file=&lt;Path to Config file&gt; lc-automatique.jar 

Having issues?
==============
Please open the new issue in the project and i'll try to response you as soon as I can.


