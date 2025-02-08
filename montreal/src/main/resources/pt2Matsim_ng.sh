#!/bin/bash
#SBATCH --nodes=1
#SBATCH --cpus-per-task=80
#SBATCH --ntasks=1
#SBATCH --time=24:00:00
#SBATCH --job-name=MontrealCalibration
#SBATCH --account=def-fciari
#SBATCH --mail-user=ashraf-uz-zaman.patwary@polymtl.ca
#SBATCH --mail-type=ALL
#SBATCH --output=/scratch/f/fciari/ashraf37/pt2Matsim/output.slurm


echo "Current working directory: `pwd`"
echo "Starting run at: `date`"


# Load Java
module load java/17.0.2

# Load script
java -Xmx150g -cp montreal-0.0.1-SNAPSHOT-jar-shaded.jar network.NetworkWithLanesTrial data/osm/RegionMontrealaise.osm data/osm/fixOSM.osm data/osm/outputNet.xml data/osm/outputLanes.xml data/kinan/gtfsData/out/ data/osm/osmTs.xml data/osm/osmVehicles.xml data/osm/osmMultimodal.xml data/osm/osmTsMapped.xml data/osm/ptMapperConfig.xml


###############################################


# ---------------------------------------------------------------------
echo "Job finished with exit code $? at: `date`"


