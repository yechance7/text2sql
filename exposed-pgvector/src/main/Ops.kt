import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.wrap

/**
 * implement pgvector operators
 */

class CosineDistanceOp<T : Number>(
    left: Expression<FloatArray>,
    right: Expression<FloatArray>,
    columnType: IColumnType<T>
) : CustomOperator<T>("<=>", columnType, left, right)

infix fun ExpressionWithColumnType<FloatArray>.cosineDistance(t: FloatArray) = CosineDistanceOp(this, wrap(t), FloatColumnType())
