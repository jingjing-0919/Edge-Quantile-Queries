# <a href="https://msca-malot.github.io/" target="_blank"><img src="https://msca-malot.github.io/images/logo-wide.png" style="width:60px; margin-right:10px;"/></a>IoT-Quantile-Queries

## Introduction

This repository encapsulates the source code, datasets, and scripts for 
reproducing of our paper entitled 
"Efficient and Error-bounded Spatiotemporal Quantile Monitoring in Edge Computing Environments".

## Acknowledgements

- The module of GK (Greenwald-Khanna) Algorithm is 
based on the [implementation](https://github.com/WladimirLivolis/GreenwaldKhanna) 
by @[WladimirLivolis](https://github.com/WladimirLivolis/GreenwaldKhanna/commits?author=WladimirLivolis).

- The real mobility dataset is created from the 
[GeoLife trajectory project](https://www.microsoft.com/en-us/download/details.aspx?id=52367&from=https%3A%2F%2Fresearch.microsoft.com%2Fen-us%2Fdownloads%2Fb16d359d-d164-469e-9fd4-daa38f2b2e13%2F).

[//]: # (Some of the baseline's code was overlapped. It means you just need to change several lines of the code to make it work different.)

[//]: # (The accurate estimation of the experiment  BaseStations  is the guarantee of  the  performance of our algorithm .&#40;it means the source data &#40;baseStations16.txt ,baseStations24.txt ,baseStations32.txt &#41; may only worked well on my computer. To get a better performance , it is recommended that  get the estimation on your machine&#41;.)

[//]: # (The FinalTestSingle.java is the main class for Single Query.)

[//]: # (The MultipleQuery2.java is the main class for concurrent Query.)

## Get Started

### Dependencies

such as Java, MVN, etc.

### Project Structure

### Execution

Step 1:

Step 2:

Step 3:...

## Result Reproduction

### Our Testing Environment

***Note that we validate the efficiency results including maximum/average query latency and EBD time in
the above-mentioned testing environment. One with a different CPU and/or memory size should get 
the results with similar trends as reported in our paper.***

### Tuning Parameters

## Citation

If you use our code for your research work, please cite our paper as follows.
```
@article{li2022efficient,
  title={Efficient and Error-bounded Spatiotemporal Quantile Monitoring in Edge Computing Environments},
  author={Li, Huan and Yi, Lanjing and Tang, Bo and Lu, Hua and Jensen, Christian S.},
  year={2022}
}
```



