# `BLOGBuilder` README

*BLOGBuilder* is a module of *ComPI* that allows the automatic creation of series of BLOG model files that ideally increase in complexity. In this regard, multiple World Filling Strategies (as we call them) are available as explained below. 
It is also possible to implement custom strategies for the creation of models and model series. 

## Usage

Due to the not (yet) final state of *BLOGBuilder*, it is not yet distributed as a executable jar file. To allow the greatest amount of flexibility possible, it is shipped as the raw Java files. In order to create series of model files, we recommend you setup a Java project (e.g., in the Eclipse IDE) and proceed as follows.

Strategy and parameter selection happen directly by setting corresponding variables in the java files:

#### Setup

##### `Main.java`: Strategy Selection

The `main` method contains the selection of a `WorldCreationStrategy` in the following lines:

```Java
// ...
//WorldCreationStrategy strat = new RandomSampleStrategy();
WorldCreationStrategy strat = new RandVarOccAugmStrategy();
//WorldCreationStrategy strat = new LogVarAugmentationStrategy();
//WorldCreationStrategy strat = new RandVarAugmentationStrategy();
//WorldCreationStrategy strat = new FactorAugmentationStrategy();
//WorldCreationStrategy strat = new IncByWorldStrategy();
//WorldCreationStrategy strat = new ParallelFactorArgsAugmentationStrategy();
// ...
```

Select a strategy by including the corresponding line (removing the `//` at the beginning of the line) and commenting out all remaining strategies (putting `//` at the beginning of the lines). 

##### `<strategy>.java`: Parameter Specification

The parameters describing the models to be created are then specified in the `.java` file corresponding to the chosen strategy. For the code snippet from above (where the `LogVarAugmentationStrategy` is selected), we would have to check out `RandVarAugmentationStrategy.java`. 

In that file, the corresponding variables `params_*` are set, e.g. specifying the number of logvars, randvars, factors, the maximum number of arguments for a randvar or factor, etc.

```java
// ...	
// Do you want to create all queries or 1 query per randvar?
private boolean allQueries = true;

// How big should each of the domains be?
private int[] domainSizes = {10,100,1000};

// How often do you want each size to be created (=multiple independent runs)?
int rerollCount = 3;

// Should (old) RandVars from factors be replaced or should the factors be augmented?
boolean replaceRandVars = false;

int[] params_randVarCounts =  {3,4,5,6,7,8,9,10,11,12,14,15,16,17,18,19,20}; 

// We go through these counts in parallel
int[] params_logVarCounts =  {3};
int[] params_factorCounts =  {5};

// We go through these counts as power set (all combinations)
int[] params_maxRandVarArgs = {2, 3}; 
int[] params_argsInFactor = {4, 5};
int[] params_maxRandVarOcc = {2, 3}; 
// ...
```

The logic describing how these parameters are processed is then described in the `paramHandler*()` method. You will need to follow the structure of the nested for loops to derive said logic, e.g., which variable sets are simply iterated 'in parallel', or which variable sets are processed as cartesian product. 

#### Build & Execution

In order to start the model creation process, we have to build it first. You can either do this with your command line and [Maven](https://maven.apache.org/download.cgi), which you need to have correctly set up, or do it directly from Eclipse (recommended). After running it, the BLOG model files are outputted to a `out/` subdirectory that is created for you, along with two reports describing the model creation process.

###### Build Process in Eclipse (*recommended*)

Import the source files as a Maven project to Eclipse (File -> Import -> Maven / Existing Maven Project -> Select the pom.xml -> Finish), set up a run configuration with the `blogbuilder.Main` class containing the main method (if needed, default might be fine) and just run the thing using the run button (play button in the menu bar).

###### Build Process with just Maven

1. Build a jar file using (from the parent *BLOGBuilder* directory):

   ```
   $ mvn compile
   $ mvn clean compile assembly:single
   ```

2. Execute said jar file (it is located in the `target` subdirectory):

   ```
   $ java -jar target/BLOGBuilder-0.0.1-SNAPSHOT-with-dependencies.jar
   ```

## World Filling Strategies

**Explanation:** `baseFactory` vs. `augmentationFactory`

We differentiate between two factory types:

* A `baseFactory` is able to fill a world with elements *from scratch* (e.g. the `RandomSampleFactory`) - creating a `baseWorld`, whereas
* a `augmentationFactory` takes an existing `baseWorld` and augments it by changing (adding, removing, switching, ...) its elements, thus creating a `augmentWorld.`
  When new elements are added, the `aF` should request the world's `baseFactory` in order for the added elements' creation to comply with the previous world filling strategy.

### Base Factories

#### `RandomSampleFactory`

`insertLogVars(...)`-Strategy:

1. Create `logVarCount` LogVars named according to the world's naming function.

`insertRandVars(...)`-Strategy:

1. Create `randVarCount` RandVars.
2. For each RandVar:
   1. Pick a random `argCount` between 0 and `maxRandVarArgs`
   2. Shuffle the LogVars and pick the `argCount` first elements as arguments for the current RandVar.

`insertFactors(...)`-Strategy:

1. Create `factorCount` factors with a randomly picked `argCount` between 1 and `factorArgCount`.  
   Only considers RandVars that are not yet connected (i.e. occur together with one of the current arg RandVars from this Factor). Might lead to smaller arg turnout than `argCount`. In that case a warning is displayed in the console.
   Args are filled with the following priority.
   1. RandVars without occurrence
   2. RandVars not yet at `maxOccurrence`
   3. RandVars at `maxOccurrence`

##### `UniformFactorArgsFactory`

> `extends RandomSampleFactory`

`insertFactors(...)`-Strategy:

1. Create base world with `l` LogVars, `r` RandVars
2. Factor creation: 
   1. Create `r-1` factors, one for each of the last `r-1` RandVars.
   2. The `0`th RandVar is added to *every* factor (to connect all factors).

### Augmentation Factories

Augmentation Factories all extend the abstract class `GenericAugmentationFactory` where some default behaviors are implemented (e.g. the `init*` methods, where all elements from a base world are copied). 

#### `LogVarAugmentationFactory`

`insertLogVars(...)`-Strategy:

1. Clone existing LogVars from base world.
2. Add new LogVars based on the base world's filling strategy.
   1. Candidate RandVars are those with at at least 1 argument LogVar.
   2. Randomly choose `rv_count` RandVars from the existing candidates, with `rv_count` as a random number between `1` and the world's `maxLVOccurence`.
   3. For each of these RandVars, randomly replace one of their current LogVars

#### `RandVarAugmentationFactory`

`insertRandVars(...)`-Strategy:

1. Clone existing RandVars from base world.
2. Add new RandVars based on the base world's filling strategy.
   1. Factors with at least one RandVar with an occurence > 1 are candidates. If none such candidate exists, all factors with at least 1 argument RandVar are taken.
   2. Randomly choose `fac_count` (par)factors from the candidates, with
      `fac_count ` as a random number between `1` and the world's `maxRVOccurence`.
   3. For each of these (par)factors, randomly replace one of the current RandVars. 

#### `FactorAugmentationFactory`

`insertFactors(...)`-Strategy:

1. Clone existing (par)factors from base world.
2. Add new (par)factors based on the base world's base filling strategy. 

#### `RandVarOccurrenceAugmentationFactory`

`insertFactors(...)`-Strategy:

1. Copy all factors from the base world to the new world `w`.
2. For each factor: Based on certain probability (e.g. 50%):
   1. Add a randomly chosen RandVar to those factors.

#### `ParallelFactorArgsAugmentationFactory`

`augmentation` strategy:

1. Copy all factors from base world to the new world w.
2. For each factor, *create* a new RandVar and take it into its arguments.
