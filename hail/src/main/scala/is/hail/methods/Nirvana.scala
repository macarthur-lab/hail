package is.hail.methods

import java.io.{FileInputStream, IOException}
import java.util.Properties

import is.hail.annotations._
import is.hail.expr.JSONAnnotationImpex
import is.hail.expr.ir.{TableLiteral, TableValue}
import is.hail.expr.types._
import is.hail.rvd.{OrderedRVD, RVDContext}
import is.hail.sparkextras.ContextRDD
import is.hail.table.Table
import is.hail.utils._
import is.hail.variant.{Locus, RegionValueVariant}
import org.apache.spark.sql.Row
import org.apache.spark.storage.StorageLevel
import org.json4s.jackson.JsonMethods

import scala.collection.JavaConverters._


object Nirvana {

  //For Nirnava v2.0.8

  val nirvanaSignature = TStruct(
    "chromosome" -> TString(),
    "refAllele" -> TString(),
    "position" -> TInt32(),
    "altAlleles" -> TArray(TString()),
    "cytogeneticBand" -> TString(),
    "quality" -> TFloat64(),
    "filters" -> TArray(TString()),
    "jointSomaticNormalQuality" -> TInt32(),
    "copyNumber" -> TInt32(),
    "strandBias" -> TFloat64(),
    "recalibratedQuality" -> TFloat64(),
    "clingen" -> TArray(TStruct(
      "chromosome" -> TString(),
      "begin" -> TInt32(),
      "end" -> TInt32(),
      "variantType" -> TString(),
      "id" -> TString(),
      "clinicalInterpretation" -> TString(),
      "observedGains" -> TInt32(),
      "observedLosses" -> TInt32(),
      "validated" -> TBoolean(),
      "phenotypes" -> TArray(TString()),
      "phenotypeIds" -> TArray(TString()),
      "reciprocalOverlap" -> TFloat64()
    )),
    "dgv" -> TArray(TStruct(
      "chromosome" -> TString(),
      "begin" -> TInt32(),
      "end" -> TInt32(),
      "variantType" -> TString(),
      "id" -> TString(),
      "variantFreqAll" -> TFloat64(),
      "sampleSize" -> TInt32(),
      "observedGains" -> TInt32(),
      "observedLosses" -> TInt32(),
      "reciprocalOverlap" -> TFloat64()
    )),
    "oneKg" -> TArray(TStruct(
      "chromosome" -> TString(),
      "begin" -> TInt32(),
      "end" -> TInt32(),
      "variantType" -> TString(),
      "id" -> TString(),
      "variantFreqAll" -> TFloat64(),
      "variantFreqAfr" -> TFloat64(),
      "variantFreqAmr" -> TFloat64(),
      "variantFreqEas" -> TFloat64(),
      "variantFreqEur" -> TFloat64(),
      "variantFreqSas" -> TFloat64(),
      "sampleSize" -> TInt32(),
      "sampleSizeAfr" -> TInt32(),
      "sampleSizeAmr" -> TInt32(),
      "sampleSizeEas" -> TInt32(),
      "sampleSizeEur" -> TInt32(),
      "sampleSizeSas" -> TInt32(),
      "observedGains" -> TInt32(),
      "observedLosses" -> TInt32(),
      "reciprocalOverlap" -> TFloat64()
    )),
    "cosmic" -> TArray(TStruct(
      "id" -> TInt32(),
      "chromosome" -> TString(),
      "begin" -> TInt32(),
      "end" -> TInt32(),
      "variantType" -> TString(),
      "copyNumber" -> TInt32(),
      "cancerTypes" -> TArray(TTuple(TString(),TInt32())),
      "tissues" -> TArray(TTuple(TString(),TInt32())),
      "reciprocalOverlap" -> TFloat64()
    )),
    "variants" -> TArray(TStruct(
      "altAllele" -> TString(),
      "refAllele" -> TString(),
      "chromosome" -> TString(),
      "begin" -> TInt32(),
      "end" -> TInt32(),
      "phylopScore" -> TFloat64(),
      "isReferenceMinor" -> TBoolean(),
      "variantType" -> TString(),
      "vid" -> TString(),
      "hgvsg" -> TString(),
      "isRecomposedVariant" -> TBoolean(),
      "isDecomposedVariant" -> TBoolean(),
      "regulatoryRegions" -> TArray(TStruct(
        "id" -> TString(),
        "type" -> TString(),
        "consequence" -> TSet(TString())
      )),
      "clinvar" -> TArray(TStruct(
        "id" -> TString(),
        "reviewStatus" -> TString(),
        "isAlleleSpecific" -> TBoolean(),
        "alleleOrigins" -> TArray(TString()),
        "refAllele" -> TString(),
        "altAllele" -> TString(),
        "phenotypes" -> TArray(TString()),
        "medGenIds" -> TArray(TString()),
        "omimIds" -> TArray(TString()),
        "orphanetIds" -> TArray(TString()),
        "significance" -> TString(),
        "lastUpdatedDate" -> TString(),
        "pubMedIds" -> TArray(TString())
      )),
      "cosmic" -> TArray(TStruct(
        "id" -> TString(),
        "isAlleleSpecific" -> TBoolean(),
        "refAllele" -> TString(),
        "altAllele" -> TString(),
        "gene" -> TString(),
        "sampleCount" -> TInt32(),
        "studies" -> TArray(TStruct(
          "id" -> TInt32(),
          "histology" -> TString(),
          "primarySite" -> TString()
        ))
      )),
      "dbsnp" -> TStruct("ids" -> TArray(TString())),
      "gnomad" -> TStruct(
        "coverage" -> TString(),
        "allAf" -> TFloat64(),
        "allAc" -> TInt32(),
        "allAn" -> TInt32(),
        "allHc" -> TInt32(),
        "afrAf" -> TFloat64(),
        "afrAc" -> TInt32(),
        "afrAn" -> TInt32(),
        "afrHc" -> TInt32(),
        "amrAf" -> TFloat64(),
        "amrAc" -> TInt32(),
        "amrAn" -> TInt32(),
        "amrHc" -> TInt32(),
        "easAf" -> TFloat64(),
        "easAc" -> TInt32(),
        "easAn" -> TInt32(),
        "easHc" -> TInt32(),
        "finAf" -> TFloat64(),
        "finAc" -> TInt32(),
        "finAn" -> TInt32(),
        "finHc" -> TInt32(),
        "nfeAf" -> TFloat64(),
        "nfeAc" -> TInt32(),
        "nfeAn" -> TInt32(),
        "nfeHc" -> TInt32(),
        "othAf" -> TFloat64(),
        "othAc" -> TInt32(),
        "othAn" -> TInt32(),
        "othHc" -> TInt32(),
        "asjAf" -> TFloat64(),
        "asjAc" -> TInt32(),
        "asjAn" -> TInt32(),
        "asjHc" -> TInt32(),
        "failedFilter" -> TBoolean()
      ),
      "gnomadExome" -> TStruct(
        "coverage" -> TString(),
        "allAf" -> TFloat64(),
        "allAc" -> TInt32(),
        "allAn" -> TInt32(),
        "allHc" -> TInt32(),
        "afrAf" -> TFloat64(),
        "afrAc" -> TInt32(),
        "afrAn" -> TInt32(),
        "afrHc" -> TInt32(),
        "amrAf" -> TFloat64(),
        "amrAc" -> TInt32(),
        "amrAn" -> TInt32(),
        "amrHc" -> TInt32(),
        "easAf" -> TFloat64(),
        "easAc" -> TInt32(),
        "easAn" -> TInt32(),
        "easHc" -> TInt32(),
        "finAf" -> TFloat64(),
        "finAc" -> TInt32(),
        "finAn" -> TInt32(),
        "finHc" -> TInt32(),
        "nfeAf" -> TFloat64(),
        "nfeAc" -> TInt32(),
        "nfeAn" -> TInt32(),
        "nfeHc" -> TInt32(),
        "othAf" -> TFloat64(),
        "othAc" -> TInt32(),
        "othAn" -> TInt32(),
        "othHc" -> TInt32(),
        "asjAf" -> TFloat64(),
        "asjAc" -> TInt32(),
        "asjAn" -> TInt32(),
        "asjHc" -> TInt32(),
        "sasAf" -> TFloat64(),
        "sasAc" -> TInt32(),
        "sasAn" -> TInt32(),
        "sasHc" -> TInt32(),
        "failedFilter" -> TBoolean()
      ),
      "topmed" -> TStruct(
        "failedFilter" -> TBoolean(),
        "allAc" -> TInt32(),
        "allAn" -> TInt32(),
        "allAf" -> TFloat64(),
        "allHc" -> TInt32()
      ),
      "globalAllele" -> TStruct(
        "globalMinorAllele" -> TString(),
        "globalMinorAlleleFrequency" -> TFloat64()
      ),
      "oneKg" -> TStruct(
        "ancestralAllele" -> TString(),
        "allAf" -> TFloat64(),
        "allAc" -> TInt32(),
        "allAn" -> TInt32(),
        "afrAf" -> TFloat64(),
        "afrAc" -> TInt32(),
        "afrAn" -> TInt32(),
        "amrAf" -> TFloat64(),
        "amrAc" -> TInt32(),
        "amrAn" -> TInt32(),
        "easAf" -> TFloat64(),
        "easAc" -> TInt32(),
        "easAn" -> TInt32(),
        "eurAf" -> TFloat64(),
        "eurAc" -> TInt32(),
        "eurAn" -> TInt32(),
        "sasAf" -> TFloat64(),
        "sasAc" -> TInt32(),
        "sasAn" -> TInt32()
      ),
      "mitomap" -> TArray(TStruct(
        "refAllele" -> TString(),
        "altAllele" -> TString(),
        "diseases"  -> TArray(TString()),
        "hasHomoplasmy" -> TBoolean(),
        "hasHeteroplasmy" -> TBoolean(),
        "status" -> TString(),
        "clinicalSignificance" -> TString(),
        "scorePercentile" -> TFloat64(),
        "isAlleleSpecific" -> TBoolean(),
        "chromosome" -> TString(),
        "begin" -> TInt32(),
        "end" -> TInt32(),
        "variantType" -> TString()
      )),
      "transcripts" -> TStruct(
        "refSeq" -> TArray(TStruct(
          "transcript" -> TString(),
          "bioType" -> TString(),
          "aminoAcids" -> TString(),
          "cdnaPos" -> TString(),
          "codons" -> TString(),
          "cdsPos" -> TString(),
          "exons" -> TString(),
          "introns" -> TString(),
          "geneId" -> TString(),
          "hgnc" -> TString(),
          "consequence" -> TArray(TString()),
          "hgvsc" -> TString(),
          "hgvsp" -> TString(),
          "isCanonical" -> TBoolean(),
          "polyPhenScore" -> TFloat64(),
          "polyPhenPrediction" -> TString(),
          "proteinId" -> TString(),
          "proteinPos" -> TString(),
          "siftScore" -> TFloat64(),
          "siftPrediction" -> TString()
        )),
        "ensembl" -> TArray(TStruct(
          "transcript" -> TString(),
          "bioType" -> TString(),
          "aminoAcids" -> TString(),
          "cdnaPos" -> TString(),
          "codons" -> TString(),
          "cdsPos" -> TString(),
          "exons" -> TString(),
          "introns" -> TString(),
          "geneId" -> TString(),
          "hgnc" -> TString(),
          "consequence" -> TArray(TString()),
          "hgvsc" -> TString(),
          "hgvsp" -> TString(),
          "isCanonical" -> TBoolean(),
          "polyPhenScore" -> TFloat64(),
          "polyPhenPrediction" -> TString(),
          "proteinId" -> TString(),
          "proteinPos" -> TString(),
          "siftScore" -> TFloat64(),
          "siftPrediction" -> TString()
        ))
      ),
      "overlappingGenes" -> TArray(TString())
    )),
    "genes" -> TArray(TStruct(
      "name" -> TString(),
      "omim" -> TArray(TStruct(
        "mimNumber" -> TInt32(),
        "hgnc" -> TString(),
        "description" -> TString(),
        "phenotypes" -> TArray(TStruct(
          "mimNumber" -> TInt32(),
          "phenotype" -> TString(),
          "mapping" -> TString(),
          "inheritance" -> TArray(TString()),
          "comments" -> TString()
        ))
      )),
      "exac" -> TStruct(
        "pLi" -> TFloat64(),
        "pRec" -> TFloat64(),
        "pNull" -> TFloat64()
      )
    ))
  )

  def printContext(w: (String) => Unit) {
    w("##fileformat=VCFv4.1")
    w("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT")
  }

  def printElement(vaSignature: Type)(w: (String) => Unit, v: (Locus, Array[String])) {
    val (locus, alleles) = v

    val sb = new StringBuilder()
    sb.append(locus.contig)
    sb += '\t'
    sb.append(locus.position)
    sb.append("\t.\t")
    sb.append(alleles(0))
    sb += '\t'
    sb.append(alleles.tail.filter(_ != "*").mkString(","))
    sb += '\t'
    sb.append("\t.\t.\tGT")
    w(sb.result())
  }

  def annotate(ht: Table, config: String, blockSize: Int): Table = {
    assert(ht.key.contains(FastIndexedSeq("locus", "alleles")))
    assert(ht.typ.rowType.size == 2)

    val properties = try {
      val p = new Properties()
      val is = new FileInputStream(config)
      p.load(is)
      is.close()
      p
    } catch {
      case e: IOException =>
        fatal(s"could not open file: ${ e.getMessage }")
    }

    val dotnet = properties.getProperty("hail.nirvana.dotnet", "dotnet")

    val nirvanaLocation = properties.getProperty("hail.nirvana.location")
    if (nirvanaLocation == null)
      fatal("property `hail.nirvana.location' required")

    val path = Option(properties.getProperty("hail.nirvana.path"))

    val cache = properties.getProperty("hail.nirvana.cache")


    val supplementaryAnnotationDirectoryOpt = Option(properties.getProperty("hail.nirvana.supplementaryAnnotationDirectory"))
    val supplementaryAnnotationDirectory = if (supplementaryAnnotationDirectoryOpt.isEmpty) List[String]() else List("--sd", supplementaryAnnotationDirectoryOpt.get)

    val reference = properties.getProperty("hail.nirvana.reference")

    val cmd: List[String] = List[String](dotnet, s"$nirvanaLocation") ++
      List("-c", cache) ++
      supplementaryAnnotationDirectory ++
      List("--disable-recomposition", "-r", reference,
        "-i", "-",
        "-o", "-")

    println(cmd.mkString(" "))

    val contigQuery: Querier = nirvanaSignature.query("chromosome")
    val startQuery = nirvanaSignature.query("position")
    val refQuery = nirvanaSignature.query("refAllele")
    val altsQuery = nirvanaSignature.query("altAlleles")
    val localRowType = ht.typ.rowType
    val localBlockSize = blockSize

    val rowKeyOrd = ht.typ.keyType.get.ordering

    info("Running Nirvana")

    val prev = ht.value.rvd.asInstanceOf[OrderedRVD]

    val annotations = prev
      .mapPartitions { it =>
        val pb = new ProcessBuilder(cmd.asJava)
        val env = pb.environment()
        if (path.orNull != null)
          env.put("PATH", path.get)

        val rvv = new RegionValueVariant(localRowType)

        it.map { rv =>
          rvv.setRegion(rv)
          (rvv.locus(), rvv.alleles())
        }
          .grouped(localBlockSize)
          .flatMap { block =>
            val (jt, proc) = block.iterator.pipe(pb,
              printContext,
              printElement(localRowType),
              _ => ())
            // The filter is because every other output line is a comma.
            val kt = jt.filter(_.startsWith("{\"chromosome")).map { s =>
              val a = JSONAnnotationImpex.importAnnotation(JsonMethods.parse(s), nirvanaSignature)
              val locus = Locus(contigQuery(a).asInstanceOf[String],
                startQuery(a).asInstanceOf[Int])
              val alleles = refQuery(a).asInstanceOf[String] +: altsQuery(a).asInstanceOf[IndexedSeq[String]]
              (Annotation(locus, alleles), a)
            }

            val r = kt.toArray
              .sortBy(_._1)(rowKeyOrd.toOrdering)

            val rc = proc.waitFor()
            if (rc != 0)
              fatal(s"nirvana command failed with non-zero exit status $rc")

            r
          }
      }
      .persist(StorageLevel.MEMORY_AND_DISK)

    val nirvanaORVDType = prev.typ.copy(rowType = localRowType ++ TStruct("nirvana" -> nirvanaSignature))

    val nirvanaRowType = nirvanaORVDType.rowType

    val nirvanaRVD: OrderedRVD = OrderedRVD(
      nirvanaORVDType,
      prev.partitioner,
      ContextRDD.weaken[RVDContext](annotations).cmapPartitions { (ctx, it) =>
        val region = ctx.region
        val rvb = new RegionValueBuilder(region)
        val rv = RegionValue(region)

        it.map { case (v, nirvana) =>
          rvb.start(nirvanaRowType)
          rvb.startStruct()
          rvb.addAnnotation(nirvanaRowType.types(0), v.asInstanceOf[Row].get(0))
          rvb.addAnnotation(nirvanaRowType.types(1), v.asInstanceOf[Row].get(1))
          rvb.addAnnotation(nirvanaRowType.types(2), nirvana)
          rvb.endStruct()
          rv.setOffset(rvb.end())

          rv
        }
      }).persist(StorageLevel.MEMORY_AND_DISK)

    new Table(ht.hc, TableLiteral(
      TableValue(
        TableType(nirvanaRowType, Some(FastIndexedSeq("locus", "alleles")), TStruct()),
        BroadcastRow(Row(), TStruct(), ht.hc.sc),
        nirvanaRVD
      )))
  }

  def apply(ht: Table, config: String, blockSize: Int = 500000): Table =
    annotate(ht, config, blockSize)
}
