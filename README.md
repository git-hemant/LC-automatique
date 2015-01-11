LC-automatique
==============

This program uses Lending Club REST API to automate investment decisions, here you can specify multiple strategies where each strategy can have it's own filters and corresponding portfolio where loans should be added in your lending club account. It also allows you to schedule at what time do you want to run the program. For e.g. in-case you want to run everytime new loans are released by LC.


How to configure 
==============
Please specify your numeric investor id and API key in the configuration file. In configuration file you can specify multiple strategies where each strategy have it's own filters and portfolio name in your lending club account. Please see the [sample configuraiton file](https://raw.githubusercontent.com/git-hemant/LC-automatique/master/docs/configuration/SampleConfiguration.txt) which have one very simple strategy. You can use any of the fields (case sensitive) used in the [loan response](https://raw.githubusercontent.com/git-hemant/LC-automatique/master/docs/response/ListedLoansResponse.json) in filters specified in the configuration file.


How to run 
==============
1) Download the [lc-automatique.jar](https://github.com/git-hemant/LC-automatique/blob/master/distrib/LC-automatique.jar?raw=true)
<br>
2) Run the program java -jar -Dconfig.file=&lt;Path to Config file&gt; lc-automatique.jar 

License and disclaimer
==============
This is open source program licensed as [Attribution-NonCommercial 4.0 International](https://creativecommons.org/licenses/by-nc/4.0/). As usual, I won't be responsible for any issues or damages related to this program.

