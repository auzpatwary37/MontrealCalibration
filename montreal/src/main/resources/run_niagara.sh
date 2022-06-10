#!/bin/bash
#SBATCH --nodes=1
#SBATCH --cpus-per-task=80
#SBATCH --ntasks=1
#SBATCH --time=24:00:00
#SBATCH --job-name=MontrealCalibration
#SBATCH --account=def-fciari
#SBATCH --mail-user=ashraf-uz-zaman.patwary@polymtl.ca
#SBATCH --mail-type=ALL
#SBATCH --output=/scratch/f/fciari/ashraf37/calibration_050522.slurm


echo "Current working directory: `pwd`"
echo "Starting run at: `date`"

#Load environment
module load CCEnv arch/avx512 StdEnv/2020

# Load Java
module load java/14.0.2

# Load script
java -Xmx180G -cp runSim.jar run.Run --config config_with_calibrated_parameters.xml

###############################################


# ---------------------------------------------------------------------
echo "Job finished with exit code $? at: `date`"


