library(sf)
library(tidyverse)

# This file contains code to download and extract street network data from AGRC,
# and transit data for UTA.


# Get AGRC Network data ==========
# The Utah AGRC street network database is available at 
#   https://gis.utah.gov/data/transportation/street-network-analysis/
#   
# For this research we downloaded the information from that file in August 2020.
# The file we downloaded is available on Box, but is not committed to the 
# repository for space reasons. This file contains code to download the archived 
# AGRC network, extract it. 
filegdb <- "data/UtahRoadsNetworkAnalysis.gdb"
if(!file.exists(filegdb)) {
  zippedgdb <- "data/agrc_network.zip"
  if(!file.exists(zippedgdb)) {
    download.file("https://byu.box.com/shared/static/dw7jhe5s7pwyr0tbzodi4tcyo41zafxa.zip",
      zippedgdb)
  }
  system2("7z", c("e", zippedgdb, str_c("-o", filegdb)) )
} 
  

#

# Pull network data from GDB ===============



