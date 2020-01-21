# `TranslateBLOG` README

*TranslateBLOG* was developed as a means to translate BLOG model files into other equivalent model representations.
It is based on the [GCFOVE implementation](https://dtai.cs.kuleuven.be/software/gcfove) by Taghipour, mainly using their parser for BLOG files. 

## Content

* [Supported output formats](#supported-output-formats)
* [Tool usage](#tool-usage)
* [Special operating modes](#special-operating-modes)
* [Query and Evidence Creation](#query-and-evidence-creation)

## Supported output formats

The supported output formats are:

* MLN (Markov Logic Network) - **standard mode** - as used by:
  * [WFOMC / Forclift](https://dtai.cs.kuleuven.be/software/wfomcForclift)
  * [Alchemy 2.0](https://code.google.com/archive/p/alchemy-2/)

* Dynamic MLNs - **dynamic mode (see below for more information)** as used by:
  * [UUMLN](https://www.uni-ulm.de/en/in/ki/inst/alumni/thomas-geier/)

* Dynamic BLOGs - as used in the [Dynamic Junction Tree](http://ifis.uni-luebeck.de/index.php?id=483&L=0) algorithm


## Tool usage {.tabset .tabset-fade .tabset-pills}
The JAR can easily be started with a `java -jar TranslateBLOG.jar` command:

```
$ java -jar TranslateBLOG.jar

>> TranslateBLOG - File converter
>  Usage: java -jar TranslateBLOG.jar [args] [<file> | <directory>]
Optional flags:
-o <s>, --output <s>          Create output file <s>
-c, --[no]console_mode        Show output in console
-f <s>, --format <s>          Select target format out of {mln, dmln}
-s, --[no]short_form          Allow short forms in output (if available)
-k <s>, --package <s>         Parser looks for classes in package <s>
-v, --[no]verbose             Print info about every world sampled
-g, --[no]debug               Print model, evidence, and queries
```

### List of arguments and explanations

- `-o <string>`, `--output <string>`: specifies an output file (when used in single file mode)
- `-c`, `--[no]console_mode`: prints the translated model files to the console 
- `-f <s>`, `--format <s>`: specifies the desired output format 
- `-s`, `--[no]short_form`: allows short forms in output (e.g. for types in MLN: `Person = {1,...,3}`)
  * Note short forms are not supported by all tools, or are semantically interpreted differently
    E.g., Forclift allows short forms, but Alchemy interprets them as integer types
- `-k <s>`, `--package <s>`:
- `-v`, `--[no]verbose`: Prints info aboout every world sampled (function taken from the original GCFOVE)
- `-g`, `--[no]debug`: Debug mode; print model, evidence and queries (taken form original GCFOVE)

## Special operating modes

### Batch Mode

When specifying a directory as input (i.e. as last argument) TranslateBLOG searches that directory for `*.blog` files and converts each of them to the specified target format. 

The translated model files are saved in a newly created directory: `path/to/input/_TranslateBLOG`


### Dynamic Mode

When specifying dynamic MLNs as output format via the `-f dmln` argument, the tool follows a special workflow:

1. The *BLOG input* should look similar to the following example:

  ```
  type Person;
  type Publication;
  type Conference;
  
  guaranteed Person p[10];
  guaranteed Publication q[3];
  guaranteed Conference c[20];
  
  
  random Boolean DoR(Timestep, Person);
  random Boolean Hot(Timestep);
  random Boolean AttC(Timestep, Conference);
  random Boolean Pub(Timestep, Person, Publication);
  
  
  // Single Timestep Tables
  parfactor Person X, Conference C. MultiArrayPotential[[8, 7, 6, 5, 4, 3, 2, 1]]
  	(Hot(@1), AttC(@1,C), DoR(@1,X));
  
  parfactor Person X, Publication P, Conference C. MultiArrayPotential[[8, 7, 6, 5, 4, 3, 2, 1]]
  	(Hot(@1), AttC(@1,C), Pub(@1,X,P));
  
  // Timestep Transition Tables 
  parfactor Person X, Publication P, Conference C. MultiArrayPotential[[8, 7, 6, 5, 4, 3, 2, 1, 8, 7, 6, 5, 4, 3, 2, 1]]
      (Hot(@1), Hot(@2), Pub(@1,X,P), AttC(@1,C));
  ```

> **Note** the following points:
>
> * *RandVars* (i.e. those that depend on time) need a `Timestep` as first argument.
> * the *Parfactor* definitions, the timestep `t` is referenced as `@1`, and timestep `t+1` is referenced as `@2`

2. *Output files*

  In dynamic mode two output files are created per input file: one `.mln` file for UUMLN, and one `.blog` file for the DJT algorithm.
  They are both stored under the same filename in the output directory `path/to/inputdirectory/_TranslateBLOG`

## Query and Evidence Creation


### Query Specification & Creation 

Note that the specification above still misses information on which query atoms should be included. 
If nothing is specified, all possible query atoms are created and appended to the final outputfile.

Query atom creation can however be managed by a JSON block at the end of the file which is commented out (in order to be ignored by the original BLOG parser).
In the JSON-Object there are multiple tags and attributes that can be specified. If one is left out or not found, default values will be used. 
Default values can be changed in the Java code, namely in the `blog.BLOGQuerySpecParser.java`.

Here is a working example of the JSON definition (belonging to the model file shown above):

> **BEWARE** --- The start and end tags (`[Query Spec]` and `[/Query Spec]`) are needed and must not be changed or omitted.  
> Also: The tag parsing is **case sensitive**, so pay attention to deviating capitalization. 

```
/* 
[Query Spec]
{
  "absMaxTime": 100,
  "maxTimeInc": 50,
  "timeSkip": 2,
  "timeDeltas": [-10,5,0],
  "queryIntervalHandling" : "maxTConversion", 
  "includeVars": ["DoR", "Hot"],
  "restrictQueries": [
      {
       "randvar": "Pub",
       "objects": [["p1", "p3", "p4"],
       ["q1", "q2"]]
      },
      {
       "randvar": "AttC",
       "objects": [["c1","c3"]]
       },
       {
       "randvar": "DoR",
       "objects": [[]]
       },
       {
    "randvar": "Hot",
    "objects": [[]]
       }
       ]
}
[/Query Spec]
*/
```

#### Explanation of Tags:

* `absMaxTime` -> *Integer* value of the maximal timestep to be evaluated.
* `maxTimeInc` -> *Integer* value specifying the increment of the maximal timestep (if multiple output values shall be created).  
  If `maxTimeInc` is omitted, just one file will be created (for the max time specified via `absMaxTime`)  
  *Example*: `absMaxTime := 100`, `maxTimeInc := 20` -> 5 files will be created for `1, 20, 40, 60, 80, 100` as maximal timestep.  
  The Output files will have their corresponding maxTime in their file name, so e.eg. 
* `timeSkip` -> *Integer* value of timestep skips within one output file.  
  E.g. `timeSkip:=2` leads to the following queries: `Hot(@1,@1), Hot(@3,@3), Hot(@5,@5), ..., Hot(@absMaxTime, @absMaxtime)`
* `timeDeltas` -> *Integer values in array* determining the time deltas between 'from' and 'to' timesteps.  
  E.g. `timeDeltas := [0,5,-5]` leads to queries: `..., Hot(@10, @5), Hot(@10, @10), Hot (@10, @15), ...`  
  Queries are only ever produced if both query timesteps ('from' and 'to') are within the specified boundaries (i.e. 1 and `absMaxTime`)
* `queryIntervalHandling` -> *String* of `{"none" (default), "cutoff", "maxTConversion"}` value specifying how query line creation when the specified interval is surpassed (e.g. "further into the future" than `maxTime`).  
    * `none` - Nothing happens. 
    * `cutoff` - Queries that go outside the interval are left out and not created.
    * `maxTConversion` - Queries that go outside the interval (left and right boundary) with their second parameter are converted as if the second parameter was the interval border.  
      Example: Query interval: `[1, 15]`, Query line: `query DoR2(x1, @5, @17)` -> `query(x1, @5, @15)`
* `includeVars` -> *Array of Strings* listing the RandVars we want to query. All guaranteed objects are included for each type argument of the RandVar. 
* `restrictQueries` -> *Array of JSON Objects* of RandVars and Objects the queries will be restricted to.  
  Those inner JSON objects must have the following tags:
    * `randVar` -> *String* specifying the RandVar name
    * `objects` -> *Array of String Arrays*. The array needs to conatin one String array for each argument type of the RandVar.  
      If all objects are required, just leave the corresponding String Array empty (but do specify an empty array!). 



> **Attention** - If `includeVars` <u>*and*</u> `restrictQueries` are specified, `includeVars` will be ignored and all information will be taken from `restrictQueries`.


### Evidence Specification & Creation

> **BEWARE** --- The start and end tags (`[Evidence Spec]` and `[/Evidence Spec]`) are needed and *must not* be changed or omitted.

The Evidence Specification is done similarly to the Query Specification:
Information is parsed from a commented-out JSON-Array consisting of JSON-Objects, each specifying the evidence that shall be created for one RandVar.

> **Note** that evidence should only be created for RandVars with at most 1 non-timestep argument.  
> E.g. `DoR(Timestep, Person)` and `Hot(Timestep)` are okay, `Pub(Timestep, Person, Publication)` is *not*.

#### How is evidence "*created*"? What's the logic behind it?
Firstly, the user can determine what percentage of the guaranteed objects shall be covered by evidence, i.e. for which percentage of the objects evidence shall be created for (`evidenceCoverage` tag).
The covered portion of the objects is then divided into `groupCount` groups. The elements of one group behave (nearly) identically with regards to their evidence status.
The only deviation in behaviour of objects in the same group can be specified via `flipProb` - a probability that any object's boolean state of each group is flipped. This can be used to break symmetries in the groups.
If the number of covered objects is smaller than the `groupCount`, the first is taken as number of groups for the following steps (and `groupCount` is thus ignored).

The evidence creation itself follows a logic of two two-state-automatons that can be parameterized by the user:

1. **True-False-Automaton**  
    An automaton that can switch between two states `true` and `false` determining the boolean value which is used in the evidence (e.g. `obs Hot(@1)= true`).  
    The state transition probabilities are set via `probF2F` P(`false -> false`) and `probT2T` P(`true -> true`). The remaining probabilities P(`false -> true`) and P(`true -> false`) are calculated as the complementary probabilities of the first two.  
    `percStartTrue` sets the percentage of groups that are set to `true` in t_0.   

2. **Show-Hidden-Automaton**  
    An automaton that can switch between two states `shown` and `hide` determining whether to print an evidence line to the final file or whether to not print it.  
    The state transition probabilities are set via `probShown2Shown` P(`shown -> shown`) and `probHide2Hide` P(`hide -> hide`). The remaining probabilities P(`show -> hide`) and P(`hide -> show`) are calculated as the complementary probabilities of the first two.   
    `percStartShown` sets the percentage of groups that are set to `shown` in t_0. 

#### Evidence Spec explanation
```
/*
[Evidence Spec]
[
  {
    "randvar": "DoR",
    "evidenceCoverage": 0.9,
    "groupCount": 10,
    "percStartTrue": 0.4,
    "probF2F": 0.2,
    "probT2T": 0.3,
    "percStartShown": 0.30,
    "probShown2Shown": 0.4,
    "probHide2Hide": 0.5,
    "flipProb": 0.0
  },
  {
    "randvar": "Hot",
    "evidenceCoverage": 0.1,
    "groupCount": 10,
    "percStartTrue": 0.4,
    "probF2F": 0.2,
    "probT2T": 0.3,
    "percStartShown": 0.30,
    "probShown2Shown": 0.4,
    "probHide2Hide": 0.5,
    "flipProb": 0.3
  },
  ]
[/Evidence Spec]
*/
```

#### Explanation of Tags:
* `randvar`: String - RandVar the evidence shall be created for
* `evidenceCoverage`: Decimal - Percentage of guaranteed objects of `randvar` that will be considered in evidence creation
* `groupCount`: Integer - Number of groups the covered objects will be divided into
* `percStartTrue`: Decimal - Percentage of groups that start in `true`-state (Automaton 1)
* `probF2F`: Decimal - Probability for a `false -> false` transition (Automaton 1)
* `probT2T`: Decimal - Probability for a `true -> true` transition (Automaton 1)
* `percStartShown`: Decimal - Percentage of groups that start in `shown`-state (Automaton 2)
* `probShown2Shown`: Decimal - Probability for a `shown -> shown` transition (Automaton 2)
* `probHide2Hide`: Decimal - Probability for a `hide -> hide` transition (Automaton 2)
* `flipProb`: Decimal - Probability that any object's boolean state (Automaton 1) in a group is flipped. Used for symmetry breaking.
