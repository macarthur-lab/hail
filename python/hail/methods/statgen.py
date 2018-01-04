from hail.api2.matrixtable import MatrixTable
from hail.expr.expression import *
from hail.genetics.ldMatrix import LDMatrix
from hail.typecheck import *
from hail.utils import wrap_to_list
from hail.utils.java import handle_py4j


@typecheck(dataset=MatrixTable,
           ys=oneof(Expression, listof(Expression)),
           x=Expression,
           covariates=listof(Expression),
           root=strlike,
           block_size=integral)
def linreg(dataset, ys, x, covariates=[], root='linreg', block_size=16):
    """Test each row for association with response variables using linear regression.

    Examples
    --------

    >>> dataset_result = methods.linreg(dataset, [dataset.pheno.height], dataset.GT.num_alt_alleles(),
    ...                                 covariates=[dataset.pheno.age, dataset.pheno.isFemale])

    Warning
    -------
    :meth:`linreg` uses the same set of columns for each phenotype, the set of
    columns for which **all** response variables and covariates are defined.

    Notes
    -----

    With the default root, the following row-indexed fields are added.
    The indexing of the array annotations corresponds to that of ``ys``.

    - **linreg.nCompleteSamples** (*Int32*) -- number of columns used
    - **linreg.AC** (*Float64*) -- sum of input values ``x``
    - **linreg.ytx** (*Array[Float64]*) -- array of dot products of each response vector ``y`` with the input vector ``x``
    - **linreg.beta** (*Array[Float64]*) -- array of fit effect coefficients, :math:`\hat\beta_1`
    - **linreg.se** (*Array[Float64]*) -- array of estimated standard errors, :math:`\widehat{\mathrm{se}}`
    - **linreg.tstat** (*Array[Float64]*) -- array of :math:`t`-statistics, equal to :math:`\hat\beta_1 / \widehat{\mathrm{se}}`
    - **linreg.pval** (*Array[Float64]*) -- array of :math:`p`-values

    Parameters
    ----------
    ys : :obj:`list` of :class:`hail.expr.expression.Expression`
        One or more response expressions.
    x : :class:`hail.expr.expression.Expression`
        Input variable.
    covariates : :obj:`list` of :class:`hail.expr.expression.Expression`
        Covariate expressions.
    root : :obj:`str`
        Name of resulting row-indexed field.
    block_size : :obj:`int`
        Number of row regressions to perform simultaneously. Larger blocks
        require more memory.

    Returns
    -------
    :class:`MatrixTable`
        Dataset with regression results in a new row-indexed field.
    """
    all_exprs = [x]

    ys = wrap_to_list(ys)

    # x is entry-indexed
    analyze(x, dataset._entry_indices, set(), set(dataset._fields.keys()))

    # ys and covariates are col-indexed
    for e in (tuple(wrap_to_list(ys)) + tuple(covariates)):
        all_exprs.append(e)
        analyze(e, dataset._col_indices, set(), set(dataset._fields.keys()))

    base, cleanup = dataset._process_joins(*all_exprs)

    jm = base._jvds.linreg(
        jarray(Env.jvm().java.lang.String, [y._ast.to_hql() for y in ys]),
        x._ast.to_hql(),
        jarray(Env.jvm().java.lang.String, [cov._ast.to_hql() for cov in covariates]),
        'va.`{}`'.format(root),
        block_size
    )

    return cleanup(MatrixTable(jm))


@handle_py4j
@typecheck(dataset=MatrixTable, force_local=bool)
def ld_matrix(dataset, force_local=False):
    """Computes the linkage disequilibrium (correlation) matrix for the variants in this VDS.

    .. include:: ../_templates/req_tvariant.rst

    .. include:: ../_templates/req_biallelic.rst

    .. testsetup::

        dataset = vds.annotate_samples_expr('sa = drop(sa, qc)').to_hail2()
        from hail.methods import ld_matrix

    **Examples**

    >>> ld_matrix = ld_matrix(dataset)

    **Notes**

    Each entry (i, j) in the LD matrix gives the :math:`r` value between variants i and j, defined as
    `Pearson's correlation coefficient <https://en.wikipedia.org/wiki/Pearson_correlation_coefficient>`__
    :math:`\\rho_{x_i,x_j}` between the two genotype vectors :math:`x_i` and :math:`x_j`.

    .. math::

        \\rho_{x_i,x_j} = \\frac{\\mathrm{Cov}(X_i,X_j)}{\\sigma_{X_i} \\sigma_{X_j}}

    Also note that variants with zero variance (:math:`\\sigma = 0`) will be dropped from the matrix.

    .. caution::

        The matrix returned by this function can easily be very large with most entries near zero
        (for example, entries between variants on different chromosomes in a homogenous population).
        Most likely you'll want to reduce the number of variants with methods like
        :py:meth:`.sample_variants`, :py:meth:`.filter_variants_expr`, or :py:meth:`.ld_prune` before
        calling this unless your dataset is very small.

    :param dataset: Variant-keyed dataset.
    :type dataset: :py:class:`.MatrixTable`

    :param bool force_local: If true, the LD matrix is computed using local matrix multiplication on the Spark driver.
        This may improve performance when the genotype matrix is small enough to easily fit in local memory.
        If false, the LD matrix is computed using distributed matrix multiplication if the number of entries
        exceeds :math:`5000^2` and locally otherwise.

    :return: Matrix of r values between pairs of variants.
    :rtype: :py:class:`LDMatrix`
    """

    jldm = Env.hail().methods.LDMatrix.apply(dataset._jvds, force_local)
    return LDMatrix(jldm)