library(dplyr)
library(ggplot2)
library(car)
library(userfriendlyscience)

alfa = 0.05

results = read.csv("masresults.csv", sep = ";", header = TRUE)

MASplot = function(xAxis, yAxis, filterQuery = "", title = "", xlabel = xAxis, ylabel = yAxis) {
  if(filterQuery == ""){
    if(title == ""){
      title = "Using all results"
    }
    data = results
  } else {
    if(title == ""){
      title = filterQuery
    }else{
      title = paste(title, "(", filterQuery,")")
    }
    data = results %>% filter_(filterQuery)
  }

  data = data %>%
    group_by_("protocolType", xAxis) %>%
    summarise_(aggregatedY = paste("mean(",yAxis,")"), x=paste("first(",xAxis,")"), se = paste("sd(",yAxis,")/sqrt(n())"), n = "n()")

  ggplot(data, aes(x=x, y=aggregatedY, col = protocolType)) +
    geom_line() +
    geom_point() +
    geom_errorbar(aes(ymin = aggregatedY - se*qt(1-alfa/2, df=n), ymax = aggregatedY + se*qt(1-alfa/2, df=n)), width = .3) +
    xlab(xlabel) +
    ylab(ylabel) +
    ggtitle(title)
}


MASplot("nbDrones", "totalProfit")
MASplot("nbWarehouses", "totalProfit")
MASplot("nbInitialClients", "totalProfit")
MASplot("nbDynamicClients", "totalProfit")
MASplot("nbWarehouses", "totalProfit", "nbDrones >= 7")
MASplot("nbInitialClients", "totalProfit", "nbDrones >= 7")
MASplot("nbDynamicClients", "totalProfit", "nbDrones >= 7")
#############################################################
# Question 1: difference in profit?
MASplot("nbDrones", "totalProfit")
MASplot("nbDynamicClients", "totalProfit")
MASplot("nbDynamicClients", "totalProfit", "nbDrones >= 7")
MASplot("nbDynamicClients", "totalProfit", "nbDrones < 7")
# <4, >7 en ertussen

# >= 7
subset = results %>% filter(nbDrones >= 7)
leveneTest(totalProfit ~ protocolType, subset)
posthocTGH(subset$totalProfit, subset$protocolType, method="games-howell");
# verschil tussen alle 3

# ertussen
subset = results %>% filter(nbDrones < 7 & nbDrones >= 4)
leveneTest(totalProfit ~ protocolType, subset)
posthocTGH(subset$totalProfit, subset$protocolType, method="games-howell");
# verschil tussen alle 3

# < 4
subset = results %>% filter(nbDrones < 4)
leveneTest(totalProfit ~ protocolType, subset)
posthocTGH(subset$totalProfit, subset$protocolType, method="games-howell");
# geen verschil tussen CNET en CNCP

# Hyptothese:
# < 4: fout: DynCNET minder profit, en CNCP=CNET
# ertussen: fout DynCNET maakt minder profit
# >= 7: correct
#############################################################


MASplot("nbDrones", "averageDeliveryTime")
MASplot("nbWarehouses", "averageDeliveryTime")
MASplot("nbInitialClients", "averageDeliveryTime")
MASplot("nbDynamicClients", "averageDeliveryTime")
MASplot("nbDrones", "averageDeliveryTime", "nbDrones >= 7")
MASplot("nbWarehouses", "averageDeliveryTime", "nbDrones >= 7")
MASplot("nbInitialClients", "averageDeliveryTime", "nbDrones >= 7")
MASplot("nbDynamicClients", "averageDeliveryTime", "nbDrones >= 7")
#############################################################
# Question 2: difference in averageDeliveryTime?
MASplot("nbDrones", "averageDeliveryTime")
#=> Consider 1, 2 en 3 drones

# 1 Drone
subset = results %>% filter(nbDrones == 1)
leveneTest(averageDeliveryTime ~ protocolType, subset)
posthocTGH(subset$averageDeliveryTime, subset$protocolType, method="games-howell");
# voor 1 Drone: geen verschil tussen CNET en CNCP, wel tussen DynCNET en andere 2

# 2 Drone
MASplot("nbDrones", "averageDeliveryTime", "nbDrones == 2")
subset = results %>% filter(nbDrones == 2)
leveneTest(averageDeliveryTime ~ protocolType, subset)
posthocTGH(subset$averageDeliveryTime, subset$protocolType, method="games-howell");
# voor 2 Drones: verschil tussen alle 3!

# 3 Drone
subset = results %>% filter(nbDrones == 3)
leveneTest(averageDeliveryTime ~ protocolType, subset)
posthocTGH(subset$averageDeliveryTime, subset$protocolType, method="games-howell");
# voor 3 Drones: verschil tussen alle 3!

# Hypothese:
# - fout voor 1 Drone, daar delivert DynCNET sneller dan andere 2, andere 2 wel gelijk
# - fout voor >1 Drone: daar delivert CNCP sneller dan CNET, niet gelijk
#############################################################


MASplot("nbDrones", "nbClientsNotDelivered")
MASplot("nbWarehouses", "nbClientsNotDelivered")
MASplot("nbInitialClients", "nbClientsNotDelivered")
MASplot("nbDynamicClients", "nbClientsNotDelivered")
MASplot("nbDrones", "nbClientsNotDelivered", "nbInitialClients < 20 & nbDynamicClients < 50")
MASplot("nbInitialClients", "nbClientsNotDelivered", "nbDrones >= 7")
MASplot("nbInitialClients", "nbClientsNotDelivered", "nbDrones < 7")
MASplot("nbDynamicClients", "nbClientsNotDelivered", "nbDrones < 7")
MASplot("nbDynamicClients", "nbClientsNotDelivered", "nbDrones >= 7")
#############################################################
# Question 3: difference in nbClientsNotDelivered?
MASplot("nbDrones", "nbClientsNotDelivered")
MASplot("nbDrones", "nbClientsNotDelivered", "nbDrones > 5")
MASplot("nbDrones", "nbClientsNotDelivered", "nbInitialClients < 20 & nbDynamicClients < 50")
# => Complex situation (interaction between Drones and clients)

# Not too many clients
subset = results %>% filter(nbInitialClients < 20 & nbDynamicClients < 50)
leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# => Difference between all 3

# 10 Drones
subset = results %>% filter(nbDrones == 10)
leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# CNET onder DynCNET

# 9 Drones
subset = results %>% filter(nbDrones == 9)
leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# geen verschil tussen CNET en DynCNET

# 4 Drones
subset = results %>% filter(nbDrones == 4)
leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# wel verschil tussen CNET en DynCNET

# 8 Drones
subset = results %>% filter(nbDrones == 8)
leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# DynCNET < CNET

# <=2 Drones
subset = results %>% filter(nbDrones <= 2)
leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# CNET = CNCP

# Hypothese:
# - Only when nbDrones is low, will CNET and CNCP have the same number of clients not delivered, otherwise, CNCP will outperform CNCP
# - When number of clients and drones increase, CNET and DynCNET can switch in number of clients delivered, in some situations they equal
#############################################################


MASplot("nbDrones", "nbMessages")
MASplot("nbWarehouses", "nbMessages")
MASplot("nbInitialClients", "nbMessages")
MASplot("nbDynamicClients", "nbMessages")
MASplot("nbDrones", "nbMessages", "protocolType %in% c('CONTRACT_NET', 'CONTRACT_NET_CONFIRMATION')")
MASplot("nbDynamicClients", "nbMessages", "protocolType %in% c('CONTRACT_NET', 'CONTRACT_NET_CONFIRMATION')")
MASplot("nbInitialClients", "nbMessages", "protocolType %in% c('CONTRACT_NET', 'CONTRACT_NET_CONFIRMATION')")
MASplot("nbWarehouses", "nbMessages", "protocolType %in% c('CONTRACT_NET', 'CONTRACT_NET_CONFIRMATION')")
#############################################################
# Question 4: difference in nbMessages?
MASplot("nbDynamicClients", "nbMessages")
MASplot("nbInitialClients", "nbMessages")
MASplot("nbDrones", "nbMessages")
MASplot("nbDrones", "nbMessages", "nbDynamicClients == 10 & nbInitialClients == 5")
# as nbClients rises, nbMessages rise exponential, instead of linear (but not research question!)
# as nbDrones rises, nbMessages drops

# little clients
subset = results %>% filter(nbDynamicClients == 10 & nbInitialClients == 5)
leveneTest(nbMessages ~ protocolType, subset)
posthocTGH(subset$nbMessages, subset$protocolType, method="games-howell");
# no difference between CNET and CNCP

# no filter
subset = results
leveneTest(nbMessages ~ protocolType, subset)
posthocTGH(subset$nbMessages, subset$protocolType, method="games-howell");
# difference between CNET and CNCP: CNCP MEER messages nodig

# Hypothese:
#   - fout: CNCP heeft wel meer messages nodig dan CNET
#   - correct: DynCNET heeft inderdaad wel meer messages nodig dan andere 2
#############################################################


MASplot("nbDrones", "totalProfit")
MASplot("nbWarehouses", "totalProfit")
MASplot("nbInitialClients", "totalProfit")
MASplot("nbDynamicClients", "totalProfit")
MASplot("nbWarehouses", "totalProfit", "nbDrones >= 7")
MASplot("nbInitialClients", "totalProfit", "nbDrones >= 7")
MASplot("nbDynamicClients", "totalProfit", "nbDrones >= 7")
MASplot("nbWarehouses", "totalProfit", "nbDrones < 5")
MASplot("nbInitialClients", "totalProfit", "nbDrones < 5")
MASplot("nbDynamicClients", "totalProfit", "nbDrones < 5")
#############################################################
# Question 5: possible to make a profit?
MASplot("nbDrones", "totalProfit")
# => genoeg drones nodig!
MASplot("nbWarehouses", "totalProfit", "nbDrones < 5 & nbDynamicClients < 50")
# => als er niet genoeg drones zijn, moeten er wel genoeg warehouses zijn

MASplot("nbInitialClients", "totalProfit", "nbDrones < 4")
MASplot("nbDynamicClients", "totalProfit", "nbDrones < 5")
# als er te veel clients zijn (voor het aantal drones), dan gaat het mis

MASplot("nbInitialClients", "totalProfit", "nbDrones == 5")
MASplot("nbDynamicClients", "totalProfit", "nbDrones == 5")
MASplot("nbDynamicClients", "totalProfit", "nbDrones < 7")
# Dynamic kent wel een breakdown!

# Hypothese:
#   - correct: given the right number of drones, warehouses and clients this is possible
#############################################################


