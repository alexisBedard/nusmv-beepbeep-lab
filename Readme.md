A benchmark for NuSMV extensions to BeepBeep 3
==============================================

| Author:      | Laboratoire d'informatique formelle |
| Version:     | 1.0                                 |
| Date:        | 2021-05-15                          |

This lab studies a formalization of event stream processing pipelines as Kripke
structures that can be handled by a model checker. More specifically, the
[BeepBeep](https://liflab.github.io/beepbeep-3) event stream processing library
has been modified in order to export chains of processors as input files for the
nuXmv model checking tool. This makes it possible to formally verify properties
on these pipelines, and opens the way to the use of such pipelines directly
within a model checker as an extension of its specification language.

The goal of this benchmark is to perform an experimental evaluation of the
proposed implementation, by measuring the execution time and memory consumption
of [nuXmv](https://nuxmv.fbk.eu/) on a number of BeepBeep pipelines and for a
sample of generic properties, with a special focus on the impact of parameters
Q (the size of the internal queues in each processor) and N (the size of the
domain for numerical variables).

The data produced by this lab is presented in part in the following research
article:

    A. Bédard, S. Hallé. (2021). Model checking of stream processing pipelines.
    Submitted to the 21st International Symposium on Temporal Representation
    and Reasoning (TIME), May 2021.

Instructions on using this repository
-------------------------------------

This repository contains an instance of LabPal, an environment for running
experiments on a computer and collecting their results in a user-friendly way.
The author of this archive has set up a set of experiments, which typically
involve running scripts on input data, processing their results and displaying
them in tables and plots. LabPal is a library that wraps around these
experiments and displays them in an easy-to-use web interface. The principle
behind LabPal is that all the necessary code, libraries and input data should be
bundled within a single self-contained JAR file, such that anyone can download
and *easily* reproduce someone else's experiments. Detailed instructions can be
found on the LabPal website, [https://liflab.github.io/labpal]

Building the benchmark
----------------------

First make sure you have the following installed:

- The Java Development Kit (JDK) to compile. The lab is developed to comply
  with Java version 6; it is probably safe to use any later version.
- [Ant](http://ant.apache.org) to automate the compilation and build process

Download the sources for the lab from
[GitHub](https://github.com/alexisBedard/nusmv-beepbeep-lab) or clone the
repository using Git.

### Dependencies

Use the Ant script to automatically download any libraries missing from your
system by typing:

    ant download-deps

This will put the missing JAR files in the `Source/dep` folder in the project's
root.

One library that is not fetched is the [forked version of
BeepBeep](https://github.com/alexisBedard/beepbeep-3), which you
must download and compile. Then, copy `beepbeep-3.jar` in the `Source/dep`
folder.

### Compiling

Once these steps have been taken care of, compile the sources by simply typing:

    ant

This will produce a file called `beepbeep-nusmv-lab.jar` in the folder.

### Location of nuXmv

The steps described here take care of compilation. However, since all the
experiments in the lab rely on external calls to the
[nuXmv](https://nuxmv.fbk.eu/) model checker, you must make sure that this
software is present on your machine and can be launched by typing `nuXmv` at the
command line. If not, the lab will complain that nuXmv is not present, and the
experiments will all fail, producing no useful result.

Running LabPal
--------------

If you want to see any plots associated to the experiments, you need to have
[GnuPlot](http://gnuplot.info) installed and available from the command line
by typing `gnuplot`.

To start the lab and use its web interface, type at the command line:

    java -jar beepbeep-nusmv-lab.jar.jar

You should see something like this:

    LabPal 2.8 - A versatile environment for running experiments
    (C) 2014-2017 Laboratoire d'informatique formelle
    Université du Québec à Chicoutimi, Canada
    Please visit http://localhost:21212/index to run this lab
    Hit Ctrl+C in this window to stop

Open your web browser, and type `http://localhost:21212/index` in the address
bar. This should lead you to the main page of LabMate's web control panel.
(Note that the machine running LabPal does not need to have a web browser.
You can open a browser in another machine, and replace `localhost` by the IP
address of the former.)

Using the web interface
-----------------------

The main page should give you more details about the actual experiments that
this lab contains. Here is how you typically use the LabPal web interface.

1. Go to the Experiments page.
2. Select some experiments in the list by clicking on the corresponding
   checkbox.
3. Click on the "Add to assistant" button to queue these experiments
4. Go to the Assistant page
5. Click on the "Start" button. This will launch the execution of each
   experiment one after the other.
6. At any point, you can look at the results of the experiments that have run so
   far. You can do so by:
   - Going to the Plots or the Tables page and see the plots and tables created
     for this lab being updated in real time
   - Going back to the list of experiments, clicking on one of them and get the
     detailed description and data points that this experiment has generated
7. Once the assistant is done, you can export any of the plots and tables to a
   file, or the raw data points by using the Export button in the Status page.

Please refer to the [LabPal website](https://liflab.github.io/labpal)
or to the Help page within the web interface for more information about
LabPal's functionalities.

Command line options
--------------------

The lab accepts the following options when started from the command line:

- `--with-stats`: gathers stats about state space size. Please note that
  enabling this option makes nuXmv run much slower. Also note that in the
  current version of the lab, no plots or tables are computed with this extra
  data. You may however view it by visiting the page of an individual
  experiment.
- `--use-nusmv`: calls `NuSMV` instead of `nuXmv`. This could be used to compare
  results of experiments with the two model checkers.

Disclaimer
----------

The LabPal *library* was written by Sylvain Hallé, Professor at Université du
Québec à Chicoutimi, Canada. However, the *experiments* contained in this
specific lab instance and the results they produce are the sole responsibility
of their author.

<!-- :maxLineLen=80: -->