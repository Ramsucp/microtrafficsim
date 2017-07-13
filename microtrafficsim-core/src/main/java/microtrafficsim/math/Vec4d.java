package microtrafficsim.math;

import microtrafficsim.utils.hashing.FNVHashBuilder;


/**
 * A vector containing four {@code double}s.
 *
 * @author Maximilian Luz
 */
public class Vec4d {
    public double x, y, z, w;


    /**
     * Constructs a new vector and initializes the {@code x}-, {@code y}- and {@code z}-components to zero, the
     * {@code w}-component to one.
     */
    public Vec4d() {
        this(0, 0, 0, 1);
    }

    /**
     * Constructs a new vector with the given values.
     *
     * @param x the {@code x}-component
     * @param y the {@code y}-component
     * @param z the {@code z}-component
     * @param w the {@code w}-component
     */
    public Vec4d(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**
     * Constructs a new vector with the given values.
     *
     * @param xy the {@code x}-and {@code z}-components.
     * @param z the {@code z}-component.
     * @param w the {@code w}-component.
     */
    public Vec4d(Vec2d xy, double z, double w) {
        this(xy.x, xy.y, z, w);
    }

    /**
     * Constructs a new vector with the given values.
     *
     * @param xy the {@code x}-and {@code z}-components.
     * @param z the {@code z}-component.
     * @param w the {@code w}-component.
     */
    public Vec4d(Vec2f xy, double z, double w) {
        this(xy.x, xy.y, z, w);
    }

    /**
     * Constructs a new vector with the given values.
     *
     * @param xyz the {@code x}-, {@code y} and {@code z}-components.
     * @param w the {@code w}-component
     */
    public Vec4d(Vec3f xyz, double w) {
        this(xyz.x, xyz.y, xyz.z, w);
    }

    /**
     * Constructs a new vector by copying the specified one.
     *
     * @param xyzw the vector from which the values should be copied.
     */
    public Vec4d(Vec4d xyzw) {
        this(xyzw.x, xyzw.y, xyzw.z, xyzw.w);
    }

    /**
     * Sets the components of this vector.
     *
     * @param x the {@code x}-component
     * @param y the {@code y}-component
     * @param z the {@code z}-component
     * @param w the {@code w}-component
     * @return this vector.
     */
    public Vec4d set(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /**
     * Sets the components of this vector.
     *
     * @param xy the {@code x}-, {@code y}- and {@code z}-component.
     * @param z the {@code z}-component.
     * @param w the {@code w}-component.
     * @return this vector.
     */
    public Vec4d set(Vec2f xy, double z, double w) {
        this.x = xy.x;
        this.y = xy.y;
        this.z = z;
        this.w = w;
        return this;
    }

    /**
     * Sets the components of this vector.
     *
     * @param xyz the {@code x}-, {@code y}-component.
     * @param w the {@code w}-component.
     * @return this vector.
     */
    public Vec4d set(Vec3f xyz, double w) {
        this.x = xyz.x;
        this.y = xyz.y;
        this.z = xyz.z;
        this.w = w;
        return this;
    }

    /**
     * Sets the components of this vector by copying the specified one.
     *
     * @param xyzw the vector from which the values should be copied.
     * @return this vector.
     */
    public Vec4d set(Vec4d xyzw) {
        this.x = xyzw.x;
        this.y = xyzw.y;
        this.z = xyzw.z;
        this.w = xyzw.w;
        return this;
    }


    /**
     * Calculates and returns the lenght of this vector. This calculation includes the {@code w}-component.
     *
     * @return the length of this vector.
     */
    public double len() {
        return (double) Math.sqrt(x * x + y * y + z * z + w * w);
    }

    /**
     * Calculates and returns the lengt of the vector, represented by {@code x}-, {@code y}- and {@code z}-components.
     *
     * @return the length of the vector described by the {@code x}-, {@code y}- and {@code z}-component.
     */
    public double len3() {
        return (double) Math.sqrt(x * x + y * y + z * z);
    }


    /**
     * Normalizes this vector. This calculation includes the {@code w}-component.
     *
     * @return this vector.
     */
    public Vec4d normalize() {
        double abs = (double) Math.sqrt(x * x + y * y + z * z + w * w);
        x /= abs;
        y /= abs;
        z /= abs;
        w /= abs;
        return this;
    }

    /**
     * Normalizes the {@code x}-, {@code y}- and {@code z}-components. This calculation ignores the {@code w}-component
     * (does neither read or set it).
     *
     * @return this vector.
     */
    public Vec4d normalize3() {
        double abs = (double) Math.sqrt(x * x + y * y + z * z);
        x /= abs;
        y /= abs;
        z /= abs;
        return this;
    }

    /**
     * Adds the given vector to this vector and stores the result in this vector. This calculation includes the
     * {@code w}-component.
     *
     * @param v the vector to add.
     * @return this vector.
     */
    public Vec4d add(Vec4d v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        this.w += v.w;
        return this;
    }

    /**
     * Adds the {@code x}-, {@code y}- and {@code z}-components of the given vector to this vector and stores the
     * result in this vector. This calculation fully ignores the {@code w}-component.
     *
     * @param v the vector to add.
     * @return this vector.
     */
    public Vec4d add3(Vec4d v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        return this;
    }

    /**
     * Subtract the given vector from this vector (i.e. {@code this - v}} and stores the result in this vector. This
     * calculation includes the {@code w}-component.
     *
     * @param v the vector to subtract.
     * @return this vector.
     */
    public Vec4d sub(Vec4d v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        this.w -= v.w;
        return this;
    }

    /**
     * Subtracts the {@code x}-, {@code y}- and {@code z}-components of the given vector from this vector
     * ({@code this - v}). This calculation fully ignores the {@code w}-component.
     *
     * @param v the vector to add.
     * @return this vector.
     */
    public Vec4d sub3(Vec4d v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        return this;
    }

    /**
     * Multiplies this vector with the specified scalar value and stores the result in this vector.
     * This calculation includes the {@code w}-component.
     *
     * @param scalar the scalar value to multiply this vector with.
     * @return this vector.
     */
    public Vec4d mul(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        this.w *= scalar;
        return this;
    }

    /**
     * Multiplies the {@code x}-, {@code y}- and {@code z}-components of this vector with the specified scalar value
     * and stores the result in this vector. This calculation fully ignores the {@code w}-component.
     *
     * @param scalar the scalar value to multiply this vector with.
     * @return this vector.
     */
    public Vec4d mul3(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        return this;
    }

    /**
     * Calculates and returns the dot-product of this vector with the specified one. This calculation includes the
     * {@code w}-component.
     *
     * @param v the vector to calculate the dot product with.
     * @return the dot product of this vector and {@code v}.
     */
    public double dot(Vec4d v) {
        return this.x * v.x + this.y * v.y + this.z * v.z + this.w * v.w;
    }

    /**
     * Calculates and returns the dot-product of the {@code x}-, {@code y}- and {@code z}-components of this vector
     * with the specified one. This calculation fully ignores the {@code w}-component.
     *
     * @param v the vector to calculate the dot product with.
     * @return the dot product of the {@code x}-, {@code y}- and {@code z}-components of this vector and {@code v}.
     */
    public double dot3(Vec4d v) {
        return this.x * v.x + this.y * v.y + this.z * v.z;
    }


    /**
     * Normalizes the given vector and returns the result. This calculation includes the {@code w}-component.
     *
     * @param v the vector to normalize.
     * @return {@code v} as (new) normalized vector
     */
    public static Vec4d normalize(Vec4d v) {
        double abs = (double) Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z + v.w * v.w);
        return new Vec4d(v.x / abs, v.y / abs, v.z / abs, v.w / abs);
    }

    /**
     * Normalizes the given vector by its {@code x}-, {@code y}- and {@code z}-components. The {@code w}-component of
     * the returned (normalized) vector is set to one.
     *
     * @param v the vector to normalize.
     * @return a new vector containing the normalized {@code x}-, {@code y}- and {@code z}-components and one as the
     * {@code w}-component.
     */
    public static Vec4d normalize3(Vec4d v) {
        double abs = (double) Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
        return new Vec4d(v.x / abs, v.y / abs, v.z / abs, 1.0);
    }

    /**
     * Adds the two given vectors and returns the result of this operation as new vector. This calculation includes the
     * {@code w}-component.
     *
     * @param a the first vector to add.
     * @param b the second vector to add.
     * @return the result of this addition, i.e. {@code a + b}.
     */
    public static Vec4d add(Vec4d a, Vec4d b) {
        return new Vec4d(a.x + b.x, a.y + b.y, a.z + b.z, a.w + b.w);
    }

    /**
     * Adds the {@code x}-, {@code y}- and {@code z}-components of the two given vectors. This calculation fully
     * ignores the {@code w}-components and sets the {@code w}-component of the returned vector to one.
     *
     * @param a the first vector to add.
     * @param b the second vector to add.
     * @return the result of the addition of the {@code x}-, {@code y}- and {@code z}-components of both vectors and
     * one for the {@code w}-component.
     */
    public static Vec4d add3(Vec4d a, Vec4d b) {
        return new Vec4d(a.x + b.x, a.y + b.y, a.z + b.z, 1.0f);
    }

    /**
     * Subtracts the two given vectors and returns the result of this operation as new vector. This calculation
     * includes the {@code w}-component.
     *
     * @param a the vector to subtract from.
     * @param b the vector to subtract.
     * @return the result of this subtraction, i.e. {@code a - b}.
     */
    public static Vec4d sub(Vec4d a, Vec4d b) {
        return new Vec4d(a.x - b.x, a.y - b.y, a.z - b.z, a.w - b.w);
    }

    /**
     * Adds the {@code x}-, {@code y}- and {@code z}-components of the two given vectors ({@code a.xyz - b.xyz}}.
     * This calculation full ignores the {@code w}-components and sets the {@code w}-component of the returned vector
     * to one.
     *
     * @param a the vector to subtract from.
     * @param b the vector to subtract.
     * @return the result of the subtraction of the {@code x}-, {@code y}- and {@code z}-components of both vectors
     * ({@code a.xyz - b.xyz}} and one for the {@code w}-component.
     */
    public static Vec4d sub3(Vec4d a, Vec4d b) {
        return new Vec4d(a.x - b.x, a.y - b.y, a.z - b.z, 1.0f);
    }

    /**
     * Multiplies the given vector with the given scalar value and returns its result. This calculation includes the
     * {@code w}-component.
     *
     * @param v      the vector to be multiplied with.
     * @param scalar the scalar to be multiplied with.
     * @return the result of this multiplication, i.e. {@code a * b}.
     */
    public static Vec4d mul(Vec4d v, double scalar) {
        return new Vec4d(v.x *= scalar, v.y *= scalar, v.z *= scalar, v.w *= scalar);
    }

    /**
     * Multiplies the {@code x}-, {@code y}- and {@code z}-components of the given vector with the given scalar value
     * and returns its result. This calculation does not modify the {@code w}-component of the specified vector.
     *
     * @param v      the vector to be multiplied with.
     * @param scalar the scalar to be multiplied with.
     * @return the result of the multiplication of the scalar value with the {@code x}-, {@code y}- and
     * {@code z}-components of the specified vector (i.e. {@code v.xyz * scalar}) and {@code v.w} for the
     * {@code w}-component.
     */
    public static Vec4d mul3(Vec4d v, double scalar) {
        return new Vec4d(v.x *= scalar, v.y *= scalar, v.z *= scalar, v.w);
    }

    /**
     * Calculates and returns the dot-product of the two specified vectors. This calculation includes the
     * {@code w}-component of both vectors.
     *
     * @param a the first vector.
     * @param b the second vector.
     * @return the dot-product of {@code a} and {@code b}
     */
    public static double dot(Vec4d a, Vec4d b) {
        return a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w;
    }

    /**
     * Calculates and returns the dot-product of the {@code x}-, {@code y}- and {@code z}-components of the given
     * vectors. This calculation fully ignores the {@code w}-component of both vectors.
     *
     * @param a the first vector.
     * @param b the second vector.
     * @return the dot product of the {@code x}-, {@code y}- and {@code z}-components of both vectors.
     */
    public static double dot3(Vec4d a, Vec4d b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Vec4d)) return false;

        Vec4d other = (Vec4d) obj;
        return this.x == other.x
                && this.y == other.y
                && this.z == other.z
                && this.w == other.w;
    }

    @Override
    public int hashCode() {
        return new FNVHashBuilder()
                .add(x)
                .add(y)
                .add(z)
                .add(w)
                .getHash();
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " {" + x + ", " + y + ", " + z + ", " + w + "}";
    }
}
