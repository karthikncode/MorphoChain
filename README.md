# MorphoChain #

Author: Karthik Narasimhan (karthikn@csail.mit.edu)

### Unsupervised Discovery of Morphological Chains ([TACL 2015](https://tacl2013.cs.columbia.edu/ojs/index.php/tacl/article/view/458)) ###

* A model for unsupervised morphological analysis that integrates orthographic and semantic views of words.
* Model consistently outperforms three state-of-the-art baselines on the task of morphological segmentation on Arabic, English and Turkish.

### Download ###
You can clone the repository and use the *production2* branch (default) for the latest code.

### Dependencies ###
  1. This project uses the LBFGS-B algorithm for optimization (the jar files for the library are included in lib/). We, however, recommend you to download and install the lbfgsb_wrapper for Java from [here](https://github.com/mkobos/lbfgsb_wrapper) since there may be additional steps for you to take for installing on Mac OSX. At the end of the install, move the files *lbfgsb_wrapper-<version>.jar* and *liblbfgsb_wrapper.so* (or *liblbfgsb_wrapper.dylib* on OSX) into the lib/ directory.
  2. *commons-lang3-3.3.2.jar* (included in lib/)

### How to Compile ###

On the Terminal: Coming soon.

You can also directly import the entire directory into IntelliJ or Eclipse and compile using the GUI.

### Sample Usage ###

Here is an example of how to run the code from the home directory of the project. The output will contain the predicted segmentations for all the words in the test file. If you do not have gold segmentations
to test against, you can just input a file with the word as its own segmentation (i.e. <word>:<word> instead of <word>:<segmentation> in each line of the file - see FORMATS.txt for details).
```bash
PARAMS_FILE=params.properties;
OUT_FILE=output.txt;
java -ea  -Djava.library.path=lib/ -classpath "./lib/*:./out/production/Morphology" Main $PARAMS_FILE >$OUT_FILE
```

### Configuration ###
Most parameters in the model can be changed in the file params.properties

### Word Vectors ###
A good tool to produce your own vectors from a raw corpus is [word2vec](https://code.google.com/p/word2vec/). You can also use any pre-existing vectors as long as they satisfy the format as specified in FORMATS.txt.

### Contact ###
This code is maintained by Karthik Narasimhan (karthikn@csail.mit.edu). Please use the issue tracker or contact me if you have any questions/suggestions.
