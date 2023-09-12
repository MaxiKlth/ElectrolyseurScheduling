# A Multi-Agent Approach Towards Optimized Load Distribution in Modular Electrolysis Plants

## Description

This project provides the source code for optimal load distribution on electrolysis modules within a modular electrolysis system. This uses the concept of *Marginal Levelized Cost of Hydrogen* (mLCOH) to determine the setpoint of the electrolysis modules. The model uses the *Alternating Direction Method of Multipliers* (ADMM) to solve the minimization problem.

## mLCOH Minimization Model

### Executing program

```bash
git clone https://github.com/YourUsername/YourProject.git](https://github.com/MaxiKlth/ElectrolyseurScheduling.git
```



### Electrolyzer Parameter
The parameters were defined according to xx: Ginsberg, Michael J., et al. "Minimizing the cost of hydrogen production through dynamic polymer electrolyte membrane electrolyzer operation." Cell Reports Physical Science 3.6 (2022). The polarization curve from the paper was used to determine the production rate as a function of power via Farraday's law.

<ul>
<li>Electrolysis Technology: Proton Exchange Membrane (PEM)</li>
<li>Electrolyzer Capacity: 8MW</li>
<li>Electricity Price: 0.06 €/kWh</li>
<li>Rated Voltage: 1.75V</li>
<li>Rated Current Densitiy: 1.7 Acm-^2</li>
<li>Minimum Operating Power: 10%</li>
<li>Maximum Operating Power: 100%</li>  
<li>Maintenance-Factor: 0.05% </li>  
<li>CapEx Electrolyzer 1: 898€/kW (2020)</li>  
<li>CapEx Electrolyzer 2: 613€/kW (Forecast 2030)</li>  
<li>CapEx Electrolyzer 3: 495€/kW (Forecast 2050)</li>  
</ul>

## Authors
<p>Vincent Henkel (Vincent.Henkel@hsu-hh.de)</p>
<p>Maximilian Kilthau (Maximilian.Kilthau@hsu-hh.de)</p>
<p>Felix Gehlhoff (Felix.Gehlhoff@hsu-hh.de)</p>
