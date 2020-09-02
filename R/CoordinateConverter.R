
library(rmarkdown)
library(tidyverse)
library(data.table)

coord <- read_csv("coordinates/hhcoord.csv")

sperson <- read_csv("coordinates/synthetic_persons2.csv")
comb <- left_join(coord,sperson, by = "household_id") 
  comb <- comb[,-23]
  colnames(comb)[2] <- "TAZ"
write_csv(comb,"coordinates/synthetic_persons_xy.csv")

fhouse <- read_csv("coordinates/final_households.csv")
comb2 <- left_join(coord,fhouse, by = "household_id")
  comb2 <- comb2[,-14]
  colnames(comb2)[2] <- "TAZ"
write_csv(comb2,"coordinates/final_households_xy.csv")

fperson <- read_csv("coordinates/final_persons.csv")
comb3 <- left_join(coord,fperson, by = "household_id")
  comb3 <- comb3[,-23]
  colnames(comb3)[2] <- "TAZ"
write_csv(comb3, "coordinates/final_persons_xy.csv")
