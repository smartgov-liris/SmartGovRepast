# SmartGov

SmartGov is a platform based on [Repast Simphony](https://repast.github.io/) to co-construct urban policies with multi-agents simulations.

## Installation of Repast Simphony

Although instructions to download and install Repast are provided on their [website](https://repast.github.io/download.html), please follow these steps to install Repast Simphony as they are more up-to-date.

Repast Simphony required a Java version of 8 or OpenJDK 8.

Download the last version of Eclipse IDE committers for your current OS from the [eclipse download page](https://www.eclipse.org/downloads/packages/release/2019-06/r/eclipse-ide-eclipse-committers).

Use the Eclipse Update Manager (under Help -> Install New Software) to install Repast and required dependencies from their respective update sites:
- Install the latest version of Groovy from their GitHub release [page](https://github.com/groovy/groovy-eclipse/wiki) (it should be https://dist.springsource.org/release/GRECLIPSE/e4.14)
- Install Repast from the update site: https://repocafe.cels.anl.gov/repos/repast
When installing from the Repast update site, you may receive an Eclipse security warning "Warning: You are installing software that contains unsigned content...". This warning may safely be ignored and is due to the Repast Eclipse plugin jars are not signed with a security certificate.

Once you have downloaded Eclipse and installed all required plugins, check that the groovy compiler version is set to 2.4.x in Preference -> Groovy -> Compiler. Other values may prevent Repast Simphony and ReLogo from working correctly. If only Groovy Compiler 2.4 was selected from the update site, then compiler level 2.4 will be the only option.

## Installation of SmartGov

This project contains all the relevant libraries and a class path already set. 

The SmartGov architecture relies on python scripts to learn relevant policies during simulation. 
To be able to use python scripts, it is required to have:
- Python
- Tensorflow
- Keras

## Learning visualisation

SmartGov is provided with a visualisation tool using dash and plotly with the following:
```
python SmartGov/extsrc/plotly dashlocation.py
```
You will need the following packages in order to launch the visualisation tool:
```
pip install dash dash_table_experiments
```
You can access the visualisation tool with:
```
localhost:8000
```

## Docs

The javadoc is available online [here](https://spageaud.github.io/SmartGovLiris).
