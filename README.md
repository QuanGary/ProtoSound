  
# ProtoSound #

Note: This repository is work-in-progress. Stay tuned for our first release.
  
![Status](https://img.shields.io/badge/Version-Experimental-brightgreen.svg) [![License](https://img.shields.io/badge/license-MIT-blue)](https://opensource.org/licenses/MIT)  

## Introduction ##

ProtoSound is a mobile application for Android smartphones that allows users to customize their sound recognition models and receive private sound feedback in a different contexts.

It adapts prototypical networks, a commonly used algorithm for few-shot image classification, to the sound classification domain while extending the traditional training pipeline to incorporate additional user-centric features for real-world deployment.

The project is inspired by previous sound awareness work with DHH users, our DHH lead author's experiences, and a survey of 472 DHH participants. According to field evaluations with a real-life dataset and an interactive mobile application, ProtoSound can be used to build sound recognition systems that support highly personalized and fine-grained sound categories, can train on-device in real-time, and can handle contextual variations in a variety of real-world contexts using few custom recordings.

This repository contains samples of 10 sound categories preferred by DHH people, procured from a high-quality online library, FreeSound, and manually cleaned by our research team:
- Appliances
- Baby Cry
- Car Honk
- Cat Meow
- Dog Bark
- Doorbell
- Fire Alarm
- Knocking
- Siren
- Water Running

Navigate to the **DeployablePythonCode** folder for the python-based implementation of ProtoSound which can be run on any python-enabled devices.

Navigate to the **StandaloneAndroidApp** folder for the android-based implementation of ProtoSound which can be run on android development platforms that meet the prerequisites specified.

[[Website](https://makeabilitylab.cs.washington.edu/project/protosound/)]  
[[Paper PDF](https://homes.cs.washington.edu/~djain/img/portfolio/Jain_ProtoSound_CHI2022.pdf)]  
[[Video](https://homes.cs.washington.edu/~djain/img/portfolio/protosound-video.mp4)]  


