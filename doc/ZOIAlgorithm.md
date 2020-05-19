
## Algorithm ZOI
We are currently defining a Zone of interest (ZOI) as a geographic area where a user spent some time, once or recurrently. This temporal classification allows us to identify geographical patterns / behaviors for users and subsequently caracterize places of life, work, leisure, etc.

This diagram quickly describes how ZOI are built/updated from visits. Part ot the algorithm is defining if a visit is elligible to be part of a ZOI (proximity and time spent), the other part uses a FIGMM to create or increment existing ZOIs (Learning phase): 

<p align="center">
  <img alt="ZOI diagram Algorithm" src="/assets/ZOIDiagram.png" width="50%">
</p>
