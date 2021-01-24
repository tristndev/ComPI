# `PInBench` README

*PInBench* is a **Bench**mark tool for **P**robabilistic **In**ference frameworks.
It facilitates timekeeping for multi-file, multi-query runs which allows comparison of the frameworks.

## Contents

* [Supported Frameworks](#supported-frameworks)
* [Operating the script](#operating-the-script)
* [Input Model Files](#input-model-files)
* [Script Outputs](#script-outputs)
* [Collected Information](#collected-information)
* [Development](#development)
* [Framework Notes](#framework-notes)
* [Licensing and Credits](#licensing-and-credits)

## Supported Frameworks

The following frameworks are supported. Please note, that for licensing reasons you might need to download them from the respective original repositories. 

* [WFOMC / Forclift](https://dtai.cs.kuleuven.be/software/wfomc) (included as `forclift-3.1.jar` with some changes in the output, see [below](#framework-notes))
* [GCFOVE](https://dtai.cs.kuleuven.be/software/gcfove) (*not* included. Needs to be supplied in the base directory as `gcfove.jar`)
  * Engines: `fove.LiftedVarElim`, `ve.VarElimEngine`
* [Alchemy 2.0](https://code.google.com/archive/p/alchemy-2/) (*just runs on Linux systems* - *not* included. Needs to be supplied in the base directory as `Alchemy_liftedinfer`. Compilation notes [below](#framework-notes))
  * Engines: `ptpe`, `lis`, `lvg`
* [LJT](https://www.ifis.uni-luebeck.de/index.php?id=590&L=0) (included as `fojt.jar`)
  * Engines: `fojt.LiftedJTEngine` (Lifted Junction Tree Algorithm, default), `jt.JTEngine` (Standard Junction Tree Algorithm) + standard GCFOVE engines(`ve.VarElimEngine`, `fove.LiftedVarElim`)
* [BLOG](https://bayesianlogic.github.io/) (*supported but was not implemented until the end because of different BLOG syntax between raw BLOG and GCFOVE - partly untested and buggy*) - not included

> Feel free to contact us, if you need assistance in locating or compiling the source files.

## Operating the script

The script should work in both Python 2.* and Python 3.*.
It can be executed using a terminal, i.e. with `python PInBench.py`.
Help can be displayed using the extra argument `-h`:

```
> python PInBench.py -h

positional arguments:
  directory             relative path to model file directory

optional arguments:
  -h, --help            show this help message and exit
  --framework {forclift,gcfove,jt,alchemy,blog}, -f {forclift,gcfove,jt,alchemy,blog}
                        framework to be benchmarked
  --verbose, -v         prints jar output and extracted information to console
                        (not to log)
  --engine {fove.LiftedVarElim,ve.VarElimEngine,fojt.LiftedJTEngine,jt.JTEngine,ptpe,lis,lvg}, -e {fove.LiftedVarElim,ve.VarElimEngine,fojt.LiftedJTEngine,jt.JTEngine,ptpe,lis,lvg}
                        inference engine to be chosen (if multiple available)
  --combinequeries, -cq
                        performs queries all at once (if possible), instead of
                        one by one
  --maxSampleSteps MAXSAMPLESTEPS, -ms MAXSAMPLESTEPS
                        maximum number of MCMC sampling steps (for Alchemy
                        sampling algos).
  --passThroughArgs PASSTHROUGHARGS, -pt PASSTHROUGHARGS
                        argument string that is simply passed through to the
                        engine. Enquoted with double quotes and presented with
                        equals sign between -pt and string -> -pt="ARGSTRING"
  --timeoutSkip, -ts    DEactivates Timeout exclude mode: if file_a.xyz leads
                        to timeout don't even start on bigger file_b.xyz with
                        b > a
  --timeout TIMEOUT, -t TIMEOUT
                        timeout (seconds) after which processes will be killed
  --java_xmx, -x        adds the java -Xmx16384M argument to the console calls
```

**Attention** - Note that the `--passThroughArgs` or `-pt` argument string needs to be presented *with double quotes* and *with an equal sign* between indicator and argument string, as in following example `[...] -pt="-example 20 -instance" [...]`.


An example commandline call would be:

```
python PInBench.py -f gcfove -e fove.LiftedVarElim -t 200 -x file_directory/
```

### Special operating modes

1. **Alchemy** - `--combinequeries`
  
   This mode is triggered via the `--combinequeries` or `-cq` argument. As a result, this happens: If multiple queries are defined in a input model file, all of them are executed en bloc, and not one by one (= 1 command line call per query).

   Multiple probabilities are represented in the csv file as a probability entry as follows: `{Query(A):0.123;Query(B):0.05}`

2. **Forclift** - `--combinequeries`

   This is the equivalent for the original Forclift `-margs` argument.
   
   *To be implemented.*
   
3. **GCFOVE** - `--combinequeries`  
   Was implemented for both main engines `ve.VarElimEngine` and `fove.LiftedVarElim`. This means that - contrary to the default mode - no "1 query per file" models are created and the model files are executed as is. 
   There is a small difference in information collection regarding the both engines:

   - `fove.LiftedVarElim` outputs 1 time per query
   - `ve.VarElimEngine` outputs 1 overall time (for all queries combined) - which is collected and inserted in the `*times.csv` for every query line.
   
4. **Lifted Junction Tree** - 
   `--combinequeries`
   Implemented for both engines `fojt.LiftedJTEngine` and `jt.JTEngine`. As for GCFOVE, no "1 query per file" models are created. Thus, the model is executed as is (with all queries at once).
The differences in information collection are as follows:
   
   * The `*_times.csv` file contains 1 line per query, which is named in the `query` column.
   * The corresponding query times (needed for that individual query) are stored in the `t_queries` column. The column `time` contains the total time, which is the same for all given query lines a file consists of. The same counts for the other non-query-specific parameters and collected information.
   
   *Running VE or LVE in LJT:*
   
   * The collected output times `{t_0, t_1, t_2}` are set to `-2` (since they are not collected as for JT algorithms) - the query times can be found in the `t_queries` column. 
     In the `-cq` mode, the time collection is as with the usual modes (see above).

### Timeout Skip Strategy

When running big numbers of different models, one might reach a limit of world size and complexity regarding the error-free / timeout-free handling of files. In this case, it is desired to skip the models beyond that threshold, which are very unlikely to run through without exceptions or timeouts. 

By default, this behavior is *enabled*. It can be disabled with the `-ts` flag in the script's arguments.

The criterium for skipping files shall are explained below in some key points:

* Worlds built with the `BLOGBuilder` framework follow this naming convention: 
  `<reroll_id>-<run_index>-<domain_size>#export-<lv>_<rv>_<fac>-[...].blog`
  (An example: `a-00-10#export-002_003_002-2_2_2.blog`)
  Everything before the `#` describe a "**setting**". Once we reach the threshold described above in a setting, the remaining (bigger, more complex) worlds shall be skipped.

* The threshold for excluding the remaining files from a *setting* is described by the following criterium:

  >  *Two subsequent files* need to have fewer successful queries than timeouts and (memory) errors combined.

## Input Model Files


| **Framework** | **Supported File types**  |
| ------------: | ------------------------- |
|      Forclift | `.mln` (\*)               |
|       Alchemy | `.mln` (\*), `.db` (\*\*) |
|        GCFOVE | `.blog`                   |

When starting the PInBench tool and selecting a benchmark and a directory, the tool will automatically search for the correct file type in that directory.

*(\*) Note, that there are some minor syntax differences between MLN files for Forclift and Alchemy:*
*Forclift supports a short form for type definitions (e.g. `persons = {1, ..., 100}`), whereas Alchemy recognizes this as an integer type.*
*Alchemy therefore needs the long form (here: `persons={p1,p2,p3,p4,p5}` and so on)*.

*(\*\*) Alchemy supports evidence input in `.db` files. Those  files are automatically searched and used, if they have the same name as the coresponding model `.mln` file.*
*Evidence is not yet supported in Forclift.*


### Translation of Model files (`.blog` -> `.mln`)

Input files of type BLOG can easily be translated into MLN-files using the *TranslateBLOG* java tool, which was developed on top of the GCFOVE parser and creates according output. 

Commands:
```
> java -jar TranslateBLOG.jar  

BLOG file converter
Usage: java -jar TranslateBLOG.jar <file1> ... <fileN>
Optional flags:
-o <s>, --output <s>          Create output file <s>
-c, --[no]console_mode        Show output in console
-k <s>, --package <s>         Parser looks for classes in package <s>
-v, --[no]verbose             Print info about every world sampled
-g, --[no]debug               Print model, evidence, and queries
```

As arguments, a list of files or directories (which will then be searched for blog files) can be given.

If evidence is present in a `.blog` model file, a corresponding `*.db` evidence file is created (which is supoorted e.g. by Alchemy).


## Script Outputs

The script creates two output files_ `YYYY_MM_DD-hh_mm_FRAMEWORK.log` and `YYYY_MM_DD-hh_mm_FRAMEWORK_times.csv` (with `YYYY_MM_DD-hh_mm` representing the date and time of the time when the script is started, and `FRAMEWORK` as name of the used framework).

* `*.log` file: Contains short logs of all mln-models that are used and every query that is executed on each of them. If something goes wrong (e.g. because the mln file is faulty or a Java Heapspace error), the java stacktrace is saved here.
* `*times.csv` file: Contains the collected data describing the engine execution (times, probabilities, tree sizes, ...). It is comma-delimited with a period as separator.
* `*overview.log` file: Contains a short overview (csv-format) with descriptions of the status of the handled files, like `all ok`, `timeout`, `NegativeArraySizeException`, `unspecified error`, ...

## Collected information

### LJT

```
filename		Filename of the model file
query			Current query / queries
P(query)		Probability of current query
|gr|			Number of factors in G grounded
|G|				Number of (par)factors in the model
|E|				Number of observations introduced
|Q|				Number of queries
width			Largest Number of (parameterised) RandVars in a node of the FOJT
size			Number of nodes in FOJT
mem				Required Memory (in kB) given by Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
VE_ops			Count of Variable Elimination operations (tot=SO,CC,CE,Pr,Ab,Sp)

## Times:
t_0				Tree construction
t_1				Entering evidence
t_2				Message passing the tree
t_queries		Time for individual queries
time			Total time
```

> *FOJT* means First Order Junction Tree

## Development

The Python script was written in an object-oriented approach, creating the following main classes:

* `BenchmarkEngine` - Abstract Super Class
  * Concrete `BenchmarkEngine` subclasses, 1 class per supported framework (e.g. `ForcliftEngine`)

* `Query` - Abstract Super Class
  * Concrete `Query` subclasses, 1 class per supported framework (e.g. `ForcliftQuery`)

The idea is that all framework specific information and handling is done on sub class level while the general structure and sequence of actions is wrapped in the abstract super classes. 

If you want to change e.g. the name of the used Forclift `.jar` file, go search for a corresponding line in the `ForcliftEngine` class constructor. 

`Query` classes wrap all information on how to construct and execute a query, which can then easily be executed via a `query.execute()` call.

## Framework Notes 

This section holds notes on the raw frameworks.
Some of the used frameworks have been slightly modified, in order to fix bugs, facilitate timing, etc.

The changed frameworks where compiled / put in a jar archive to make them as portable as possible. 

### Forclift

The **Forclift** implementation was modified in order to facilitate the postprocessing of outputted information:

* Change the WFOMC code so that all relevant times are printed in every execution (without the need to turn debug mode on - which slows the system). The following times are now displayed (in *milliseconds*):

  * Evidence circuit compilation time
  * Query circuit compilation time
  * Inference time

  The corresponding scala code can be found here in these files:

  * `cli > InferenceCLI.scala`
  * `inference > QueryProbAlgos.scala`

* Modify the console outputs (showing times, tree size etc.) so it's easier to parse later on.
  * `Compilation of evidence circuit [ms] -> t_evidence`
  * `Compilation of query circuit [ms] -> t_query`
  * `Inference time [ms] -> t_inference`
  * `evidence nnf size -> ev_nnf_size`
  * `evidence smooth nnf size -> ev_smooth_nnf_size`
  * `query nnf size -> q_nnf_size`
  * `query smooth nnf size -> q_smooth_nnf_size`
  * `Z`, `Z(query)`, `P(queryString)` stay the same.

### GCFOVE

The GCFOVE jar is based on a de- and recompiled build. 

One of the key changes is in the class `LiftedVarElim.java`. The following lines of the `shatter` where replaced:

```java
for (Parfactor p : bag.parfactors()) {
  Set<Parfactor> newPFs = p.makeConstraintsNormalForm();
  for (Parfactor newpf : newPFs)
    p = p.simplify();
  parfactors.addAll(newPFs);
} 
```

Replacement:
```java
for (Parfactor p : bag.parfactors()) {
  Set<Parfactor> newPFs = p.makeConstraintsNormalForm();
  for (Parfactor newpf : newPFs)
    parfactors.add(newpf.simplify());
```


### Alchemy

In Alchemy, very little was changed. Only a second timer with milliseconds resolution was integrated. 

It was compiled on a virtual machine running Fedora Core 7, with the following requirements:
Bison 2.3, Flex 2.5.4, g++ 4.1.2, Perl 5.8.8

The compiled result is a `ELF 32-bit LSB executable, Intel 80386` file, that isn't natively supported on a 64 bit Linux system. 
To make in executable, the following two libraries are needed: `libc6-i386` & `lib32stdc++6`.

Also, if errors occur, you might need to give it 'executable' rights via right click & options (or `chmod [...]`).

### (Lifted) Junction Tree

Nothing was changed in the Junction Tree implementation developed at IFIS of University Lübeck, downloaded from the webpage linked above.

## Licensing and Credits

The following software (which is included to this repository) is licensed on its own:

* The integral parts of *PInBench*:
  Licensed under Apache License 2.0 - [Link to license](LICENSE.md)

* [WFOMC / Forclift](https://dtai.cs.kuleuven.be/software/wfomc):
  Licensed under Apache License 2.0 - [Link to license](https://github.com/UCLA-StarAI/Forclift/blob/master/LICENSE)

* [LJT](https://www.ifis.uni-luebeck.de/index.php?id=590&L=0)- [Link to license](https://www.ifis.uni-luebeck.de/index.php?id=590&L=0):

  ```
  (c) 2016, 2017, 2018, 2019 Institute of Information Systems (IFIS) Universität zu Lübeck. All rights reserved.
  
  Redistribution and use in binary forms, with or without modification, are permitted provided that the following conditions are met:
  
  Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. The name of the author may not be used to endorse or promote products derived from this software without specific prior written permission.
  
  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  
  Third Party Software: LJT-Engine uses the BLOG inference system by Brian Milch et.al. that is distributed under its own license. Similar to BLOG, the LJT Engine also includes other third-party software. These third-party packages have their own licenses in the license folder.
  
  A modified version of the CUP v0.11b parser generator
  A modified version of the JFLex 1.6.0 lexical analyzer generator
  The JAMA 1.0.3 matrix package
  The JUnit 4.10 unit testing framework [http://www.eclipse.org/legal/epl-v10.html] (source code available from junit.org)
  This distribution also includes the GC-FOVE system by Nima Taghipour.
  ```

  