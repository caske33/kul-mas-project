library(dplyr)
library(ggplot2)
library(car)

results = read.csv("masresults.csv", sep = ";", header = FALSE, col.names = c("chargesInWarehouse", "protocolType", "nbWarehouses", "nbDrones", "nbInitialClients", "nbDynamicClients", "nbCrashes", "totalProfit", "nbClients", "nbClientsNotDelivered", "averageDeliveryTime", "nbDrones", "averageDistanceTravelledPerDrone", "maximumNbOrdersPerDrone", "estimatedTotalProfit", "estimatedNbCrashes", "nbMessages", "averageNbCallsForProposals", "nbCrashesByBattery"))

MASplot = function(xAxis, yAxis) {
  results %>%
    group_by_("protocolType", xAxis) %>%
    summarise_(aggregatedY = yAxis, x=paste("first(",xAxis,")")) %>%
    ggplot(aes(x=x, y=aggregatedY, col = protocolType)) +
      geom_line() +
      geom_point() +
      xlab(xAxis) +
      ylab(yAxis) +
      ggtitle("All results")
}
MASplotFilter = function(xAxis, yAxis, filterQuery) {
  results %>%
    filter_(filterQuery) %>%
    group_by_("protocolType", xAxis) %>%
    summarise_(aggregatedY = yAxis, x=paste("first(",xAxis,")")) %>%
    ggplot(aes(x=x, y=aggregatedY, col = protocolType)) +
      geom_line() +
      geom_point() +
      xlab(xAxis) +
      ylab(yAxis) +
      ggtitle(filterQuery)
}
MASplot("nbDrones", "mean(totalProfit)")
MASplot("nbWarehouses", "mean(totalProfit)")
MASplot("nbInitialClients", "mean(totalProfit)")
MASplot("nbDynamicClients", "mean(totalProfit)")

MASplotFilter("nbWarehouses", "mean(totalProfit)", "nbDrones >= 7")
MASplotMinDrones("nbInitialClients", "mean(totalProfit)", "nbDrones >= 7")
MASplotMinDrones("nbDynamicClients", "mean(totalProfit)", "nbDrones >= 7")

MASplot("nbDrones", "mean(averageDeliveryTime)")
MASplot("nbWarehouses", "mean(averageDeliveryTime)")
MASplot("nbInitialClients", "mean(averageDeliveryTime)")
MASplot("nbDynamicClients", "mean(averageDeliveryTime)")

MASplotFilter("nbDrones", "mean(averageDeliveryTime)", "nbDrones >= 7")
MASplotFilter("nbWarehouses", "mean(averageDeliveryTime)", "nbDrones >= 7")
MASplotFilter("nbInitialClients", "mean(averageDeliveryTime)", "nbDrones >= 7")
MASplotFilter("nbDynamicClients", "mean(averageDeliveryTime)", "nbDrones >= 7")

MASplotFilter("nbClients", "mean(totalProfit)", "nbDrones >= 8")




s = results %>% filter(nbDrones == 8 & nbWarehouses == 10)
# Test for homoskedasticiteit: mag niet significant zijn, Pr(>F) moet >0.05 zijn, of er mogen dus geen sterretje(s) naast resultaat staan
# Anders werkt TukeyHSD NIET!
leveneTest(totalProfit ~ protocolType, s)
s.aov = aov(totalProfit ~ protocolType, s)
summary(s.aov)
TukeyHSD(s.aov)

s = results %>% filter(nbDrones == 3 & nbDynamicClients == 40 & nbInitialClients == 10 & nbWarehouses == 8)
boxplot(totalProfit ~ protocolType, s)
leveneTest(totalProfit ~ protocolType, s)
s.aov = aov(totalProfit ~ protocolType, s)
summary(s.aov)
TukeyHSD(s.aov)
